import com.google.gson.Gson;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

class Utility {
    // Legge dal socket e inserisce nel buffer. Ritorna il numero di byte letti o exception
    static int readJson(SocketChannel daServire , ByteBuffer buffer) throws IOException {
        int letto = 0;
        while(buffer.hasRemaining()){
            int tmp = daServire.read(buffer);
            if(tmp <= 0)
                break;
            letto += tmp;
        }
        return letto;
    }

    // Determina il filePath di una sezione di un documento
    private static String getFilePath(Documento d,int num){
        String path;
        String[] split;
        if(d.getNumsezioni()==1 || num == 0){
            path = "DATA/"+d.getOwner()+"/"+d.getNomefile();
        }else {
            split = d.getNomefile().split("\\.");
            path = "DATA/"+d.getOwner()+"/"+split[0]+"_"+num+"."+split[1];
        }
        return path;
    }

    // Crea un file per il documento specifico
    static int creaFile(Documento d){
        FileChannel documento;
        String path;
        if(Files.exists(Paths.get(getFilePath(d,0)).toAbsolutePath()) ||
                Files.exists(Paths.get(getFilePath(d,1)).toAbsolutePath()))
            return -1;
        for (int i=1;i<=d.getNumsezioni() ; i++) {
            path = getFilePath(d,i);
            try {
                documento = FileChannel.open(Paths.get(path).toAbsolutePath(), StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
                return 1;
            }
            try {
                documento.close();
            } catch (IOException ignored){}
        }
        return 0;
    }

    // Invia disponibilità notifica all'utente (Inviti, Disponibilità)
    static void sendNotification(SocketChannel daServire, String user, String msg, boolean onlineOnly){
        SocketChannel send = TuringServer.online.get(user);
        // Se onlineOnly true  e destinatario offline non invio (Disponibilità offline)
        if(onlineOnly && (send == null))
            return;
        // In tutti gli altri casi va inviata (Inviti)

        // Aggiunge norifica per l'utente
        TuringServer.usersHandler.getRegisteredUser(user).addNotifica(msg);

        // Invio lista e notifica se utente online
        if(send!=null){
            // Lock per socket se la notifica e' per un altro utente, altrimenti ho gia il lock sul sock
            ReentrantLock mutex_destinatario = null;
            if (send != daServire){
                // Recupera lock destinatario notifica
                mutex_destinatario = TuringServer.lockChannel.get(send);
                // Acquisice la lock
                mutex_destinatario.lock();
            }
            Utility.sendResponse(send, new RispostaTCP(0, TuringServer.usersHandler.getUserDocuments(user),
                            TuringServer.usersHandler.getRegisteredUser(user).getNotificationList()),
                            new RichiestaTCP(7,null));
            // libero la lock se l'ho acquisita
            if (mutex_destinatario != null)
                mutex_destinatario.unlock();
        }
    }

    static void closeChannel(SocketChannel daServire){
        try {
            SelectionKey toCancel = daServire.keyFor(TuringServer.serverSelector);
            toCancel.cancel();
            daServire.close();
        }catch(IOException ignored) { }
    }

    static Documento recuperaDocumento(RichiestaTCP richiesta, Utente user){
        int notFilename;
        boolean owned = true;
        String nomeFile = richiesta.getNomefile();
        if((notFilename = nomeFile.indexOf(" - (")) != -1){
            nomeFile = nomeFile.substring(0,notFilename);
            owned = false;
        }
        return TuringServer.usersHandler.getDoc(user,nomeFile,owned);
    }

    static void inviaDocumento(SocketChannel daServire, Documento doc, int sezione) throws IOException{
        FileChannel tmp_documento ;
        ByteBuffer len;
        Path filePath = Paths.get(getFilePath(doc,sezione)).toAbsolutePath();
        long temp,filelen;
        try{
            if(sezione == 0){
                doc.lockAllSection.lock();
                try {
                    tmp_documento = FileChannel.open(filePath,
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
                } catch (Exception e) {
                    e.printStackTrace();
                    Files.delete(filePath);
                    doc.lockAllSection.unlock();
                    return;
                }
                if(tmp_documento.size() == 0){
                    for (int i = 1; i <= doc.getNumsezioni(); i++) {
                        String path = getFilePath(doc, i);
                        FileChannel documento = FileChannel.open(Paths.get(path).toAbsolutePath(), StandardOpenOption.READ);
                        temp = 0;
                        do {
                            temp += documento.transferTo(temp, documento.size(), tmp_documento);
                        } while (temp != documento.size());
                        String s = "\n---- Fine Sezione " + i + " ----";
                        if (i != doc.getNumsezioni())
                            s = s + "\n";
                        temp = 0;
                        ByteBuffer lunghezza = ByteBuffer.allocate(s.getBytes().length);
                        lunghezza.put(s.getBytes()).flip();
                        do {
                            temp += tmp_documento.write(lunghezza);
                        } while (temp != s.getBytes().length);
                        documento.close();
                    }
                }
                tmp_documento.close();
            }
            tmp_documento = FileChannel.open(filePath, StandardOpenOption.READ);
            filelen = tmp_documento.size();
            len = (ByteBuffer) ByteBuffer.allocate(8).putLong(filelen).flip();
        }catch(Exception e) {
            // Gracefull Termination
            len = (ByteBuffer) ByteBuffer.allocate(8).putLong(0).flip();
            temp = 0;
            do{
                temp += daServire.write(len);
            }while ( temp != 8);
            if(sezione == 0) {
                Files.delete(filePath);
                doc.lockAllSection.unlock();
            }
            return;
        }
        temp = 0;
        do{
            temp += daServire.write(len);
        }while( temp != 8);
        temp = 0;
        do{
            temp += tmp_documento.transferTo(temp,filelen,daServire);
        }while ( temp != filelen);
        tmp_documento.close();
        if(sezione == 0) {
            if(doc.lockAllSection.getQueueLength() == 0)
                Files.delete(filePath);
            doc.lockAllSection.unlock();
        }
    }

    static int riceviDocumento(SocketChannel daServire, Documento doc, int sez) throws IOException{
        ReentrantLock mutex = TuringServer.lockChannel.get(daServire);
        mutex.lock();
        Path filePath = Paths.get(getFilePath(doc,sez)).toAbsolutePath();
        // Crea FileChannel in ricezione
        FileChannel incoming;
        try {
            incoming = FileChannel.open(filePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Errore ricezione file : "+filePath.toString());
            e.printStackTrace();
            mutex.unlock();
            return -4;
        }

        ByteBuffer len = ByteBuffer.allocate(8);
        // Legge la dimensione attesa
        try {
            int temp;
            do{
                temp = daServire.read(len);
            }while(temp != 8);
        } catch (IOException e) {
            System.err.println("Errore ricezione file : "+filePath.toString());
            e.printStackTrace();
            mutex.unlock();
            return -4;
        }
        // Dimensione attesa
        long incomingSize = ((ByteBuffer)len.flip()).getLong();
        long tmp = 0;
        // Avvia trasferimento documento via filechannel
        do{
            tmp += incoming.transferFrom(daServire,tmp,incomingSize);
        }while ( tmp != incomingSize);
        incoming.close();
        mutex.unlock();
        return 0;
    }

    static void sendResponse(SocketChannel daServire, RispostaTCP risposta, RichiestaTCP richiesta){
        byte[] byte_risposta = (new Gson()).toJson(risposta).getBytes();
        // Alloca ByteBuffer per contenere la risposta
        ByteBuffer buffer = (ByteBuffer) ByteBuffer.allocate(byte_risposta.length).put(byte_risposta).flip();
        ByteBuffer len = (ByteBuffer) ByteBuffer.allocate(4).putInt(byte_risposta.length).flip();
        // Scrive nel channel
        try {
            while (len.hasRemaining())
                daServire.write(len);
            while (buffer.hasRemaining())
                daServire.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(richiesta.getCodop() == 1 ){
            // Se esito positivo associo a SocketChannel il relativo username altrimenti chiudo connessione
            if (risposta.getEsito() == 0)
                TuringServer.online.put(richiesta.getUsername(),daServire);
            else
                closeChannel(daServire);
        }
    }

    static void logout(SocketChannel daServire, String username){
        // Recupero l'username se non passato come parametro
        if(username == null){
            // Se il channel esiste allora riesco a ricavare l'username
            for (Map.Entry<String, SocketChannel> entry : TuringServer.online.entrySet()) {
                if (entry.getValue() == daServire) {
                    username = entry.getKey();
                    break;
                }
            }
            // Ricerca fallita
            if(username == null)
                return;
        }
        // Controllo che non stia editando, in quest ultimo caso
        // i documenti che sarebbero dovuti essere interessati da editing rimarranno invariati
        undoRunningEdit(daServire,username);

        // Se il channel non è ancora stato chiuso l'utente esiste e deve essere sloggato
        TuringServer.usersHandler.getRegisteredUser(username).setOffline();
        TuringServer.online.remove(username);
        TuringServer.lockChannel.remove(daServire);
        // Stampa lato server
        System.out.println("Log out ok: " + username);
    }

    private static void undoRunningEdit(SocketChannel daServire, String user){
        // Se client crasha libera l'editing del file su cui stava lavorando
        for(Documento d : TuringServer.usersHandler.getUserDocuments(user)){
            // Per ogni documento non di proprietà dell'utente recupera il vero documento
            if(d.getOwner().compareTo(user) != 0){
                Utente owner = TuringServer.usersHandler.getRegisteredUser(d.getOwner());
                d = TuringServer.usersHandler.getDoc(owner,d.getNomefile(),true);
            }
            for(int i = 1; i<= d.getNumsezioni(); i++){
                String edituser = d.getEditingUser(i);
                // Se il documento è in editing da user
                if(edituser != null && edituser.compareTo(user) == 0) {
                    List<String> lst = d.removeEdit(i);
                    if (lst != null) {
                        String ntf = "Il documento: " + d.getNomefile() + " sezione: "
                                + i + " è ora disponibile per la modifica.";
                        // Notifica a chi voleva editare il file solo se ancora online
                        for (String usr : lst)
                            sendNotification(daServire, usr, ntf, false);
                        // Richiede cancellazione lista attesa
                        d.clearAttesa(i);
                    }
                    reuseMulticastAddress(d);
                }
            }
        }
    }

    static String getMulticastAddress(Documento d){
        String addr = TuringServer.docAddress.get(d);
        if(addr == null){
            addr = "239.1.1.";
            if(TuringServer.stackOne.isEmpty()){
                System.err.println("Non sono disponibili indirizzi di multicast.");
                return null;
            }else{
                addr = addr+TuringServer.stackOne.pollFirst();
            }
        }
        TuringServer.docAddress.put(d,addr);
        System.err.println("Assegnato a documento: "+d.getNomefile()+" indirizzo multicast: "+addr);
        return addr;
    }

    static void reuseMulticastAddress(Documento d){
        for(int i=1;i<d.getNumsezioni();i++){
            if(d.getEditingUser(i) != null)
                return;
        }
        String addr = TuringServer.docAddress.remove(d);
        if(addr == null)
            return;
        String[] vett = addr.split("\\.");
        TuringServer.stackOne.push(Integer.parseInt(vett[3]));
        System.err.println("Rimosso a documento: "+d.getNomefile()+" indirizzo multicast: "+addr);
    }
}
