import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class WriteTask implements Runnable{
    private SocketChannel daServire;
    private RichiestaTCP richiesta;

    WriteTask(SocketChannel sc, RichiestaTCP rc){
        daServire = sc;
        richiesta = rc;
    }

    public void run() {
        // Recupera e richiede lock su channel
        ReentrantLock mutex = TuringServer.lockChannel.get(daServire);
        mutex.lock();
        // Recupera info utente richiedente ed eventuale condivisore
        Utente currentUser = TuringServer.usersHandler.getRegisteredUser(richiesta.getUsername());
        Utente share = TuringServer.usersHandler.getRegisteredUser(richiesta.getCondivisore());
        // Analizza la richiesta e risponde
        switch (richiesta.getCodop()) {
            case 1: { // Login
                int esito = TuringServer.usersHandler.login(richiesta.getUsername(), richiesta.getPassword());
                Utility.sendResponse(daServire,new RispostaTCP(esito),richiesta);
                // Stampe lato server
                if (esito != 0) {
                    System.out.println("Login Fallito: " + richiesta.getUsername());
                } else {
                    System.out.println("Login OK: " + richiesta.getUsername());
                }
                break;
            }
            case 2: { // Crea Documento
                Documento nuovo = new Documento(richiesta.getUsername(), richiesta.getNomefile(), richiesta.getNumsezioni());
                // Crea un file vuoto in FS
                int esito = Utility.creaFile(nuovo);
                if (esito == 0) {
                    // Aggiungo documento a lista personale utente
                    TuringServer.usersHandler.addDoc(currentUser,nuovo);
                    // Stampa lato server
                    System.out.println("Creazione Documento: " + richiesta.getNomefile() + " Utente: " + richiesta.getUsername());
                }else {
                    // Stampa lato server
                    System.out.println("FALLIMENTO Creazione Documento: " + richiesta.getNomefile() + " Utente: " + richiesta.getUsername());
                }
                Utility.sendResponse(daServire,new RispostaTCP(esito),richiesta);
                break;
            }
            case 3 :{ // Modifica sezione
                Documento d = TuringServer.usersHandler.getDoc(currentUser,richiesta.getNomefile());
                // Devo lavorare sul'oggetto documento dell'owner non sulla "copia" del condivisore
                Utente owner = TuringServer.usersHandler.getRegisteredUser(d.getOwner());
                RispostaTCP resp = null;
                if( owner != null){
                    if (d != null){
                        // Prova a richiedere modifica della sezione
                        String useredit = d.addEdit(richiesta.getUsername(),richiesta.getNumsezioni());
                        // Nessuno sta gia modificando la sezione
                        if(useredit == null){
                            // Modifica possibile, invio esito positivo e file
                            List<String> lst = new ArrayList<>();
                            lst.add(TuringServer.getMulticastAddress(d));
                            Utility.sendResponse(daServire, new RispostaTCP(0,null,lst),richiesta);
                            try {
                                Utility.inviaDocumento(daServire,d,richiesta.getNumsezioni());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            //Stampa lato server
                            System.out.println("Utente: "+richiesta.getUsername()+" ha richiesto modifica file: "+
                                    richiesta.getNomefile());
                        }else{
                            List<String> info = new ArrayList<>();
                            String str = "Utente "+useredit+" sta editando la sezione del documento richiesta.";
                            // Stampa lato server
                            System.out.println("Utente "+useredit+" ha richiesto una sezione già in edit");
                            info.add(str);
                            resp = new RispostaTCP(-1,null,info);
                        }
                    }else{
                        String str = "Il documento richiesto non esiste";
                        // Stampa lato server
                        System.out.println("Utente "+richiesta.getUsername()+" "+str);
                        List<String> info = new ArrayList<>();
                        info.add(str);
                        resp = new RispostaTCP(-2,null,info);
                    }
                }else{
                    // Stampa lato server
                    String str = "Nome utente non valido";
                    System.out.println("Edit sezione: "+str);
                    List<String> info = new ArrayList<>();
                    info.add(str);
                    resp = new RispostaTCP(-3,null,info);
                }
                if(resp != null)
                    Utility.sendResponse(daServire,resp,richiesta);
                break;
            }
            case 4 :{ // Visualizza sezione
                RispostaTCP resp = null;
                if( currentUser != null) {
                    Documento d = TuringServer.usersHandler.getDoc(currentUser,richiesta.getNomefile());
                    if (d != null) {
                        try {
                            Utility.sendResponse(daServire, new RispostaTCP(0), richiesta);
                            Utility.inviaDocumento(daServire, d, richiesta.getNumsezioni());
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                        // Invio notifica per informarlo di eventuali utenti che stanno modificando
                        String ntf;
                        if ((ntf = d.getEditingNotificationString(richiesta.getNumsezioni())) != null)
                            Utility.sendNotification(daServire, richiesta.getUsername(), ntf,true);
                        // Stampe lato server OP
                        if (richiesta.getNumsezioni() == 0)
                            System.out.println("File " + richiesta.getNomefile() + " tutte le sezioni inviate a " +
                                    richiesta.getUsername());
                        else
                            System.out.println("File " + richiesta.getNomefile() + " sezione " +
                                    richiesta.getNumsezioni() + " inviato a " + richiesta.getUsername());
                    }else{
                        List<String> info = new ArrayList<>();
                        info.add("Il documento richiesto non esiste");
                        resp = new RispostaTCP(-2,null,info);
                    }
                }else{
                    List<String> info = new ArrayList<>();
                    info.add("Nome utente non valido");
                    resp = new RispostaTCP(-3,null,info);
                }
                if(resp != null)
                    Utility.sendResponse(daServire, resp,richiesta);
                break;
            }
            case 5: { // Aggiungi condivisore
                if (share == null) {
                    Utility.sendResponse(daServire, new RispostaTCP(-1),richiesta);
                    // Stampa lato server
                    System.out.println("La richiesta di condivisione dell'utente "+richiesta.getUsername()+
                            "non e' valida.");
                    break;
                }
                // Recupero il documento
                Documento d = TuringServer.usersHandler.getDoc(currentUser,richiesta.getNomefile());
                if(d != null){
                    // L'utente e' gia un condivisore del documento
                    if(!d.addShare(richiesta.getCondivisore()))
                        Utility.sendResponse(daServire, new RispostaTCP(1),richiesta);
                    else{// La richiesta puo' essere soddisfatta
                        // Aggiunge documento al nuovo condivisore
                        TuringServer.usersHandler.addDoc(share, d);
                        // Comunico esito positivo
                        Utility.sendResponse(daServire, new RispostaTCP(0),richiesta);
                        // Invio notifica al nuovo utente abilitato
                        String messaggio = "File " + richiesta.getNomefile() + " condiviso da "+
                                        richiesta.getUsername();
                        Utility.sendNotification(daServire, richiesta.getCondivisore(),messaggio,false);
                        // Informa altri client online del nuovo condivisore
                        for(String user : d.getSharedWith()){
                            if(!user.equals(richiesta.getUsername()) && !user.equals(richiesta.getCondivisore()))
                                Utility.sendNotification(daServire, user,null,true);
                        }
                        // Stampa lato server
                        messaggio = messaggio + " a "+richiesta.getCondivisore();
                        System.out.println(messaggio);
                    }
                }else{
                    // Documento non esiste
                    Utility.sendResponse(daServire, new RispostaTCP(-2),richiesta);
                    // Stampa lato server
                    System.out.println("L'utente: " + richiesta.getUsername() +
                            " ha tentato di condividere un documento non esistente.");
                }
                break;
            }
            case 7: { // Richiesta Lista
                String username = richiesta.getUsername();
                Utente user = TuringServer.usersHandler.getRegisteredUser(richiesta.getUsername());
                Utility.sendResponse(daServire, new RispostaTCP(0,
                                TuringServer.usersHandler.getUserDocuments(username),
                                user.getNotificationList()), richiesta);
                // Stampa lato server
                System.out.println("Lista inviata a: " + username);
                break;
            }
            case 8: { // Logout
                Utility.sendResponse(daServire, new RispostaTCP(0),richiesta);
                Utility.logout(daServire, richiesta.getUsername());
                Utility.closeChannel(daServire);
                break;
            }
            case 9 : // END-EDIT e Update Sezione
            case 10 :{ // END-EDIT
                Documento d = TuringServer.usersHandler.getDoc(currentUser,richiesta.getNomefile());
                RispostaTCP resp = null;
                if(d.getEditingUser(richiesta.getNumsezioni()).equals(richiesta.getUsername())){
                    List<String> lst = d.removeEdit(richiesta.getNumsezioni());
                    // END EDIT + UPDATE
                    Utility.sendResponse(daServire, new RispostaTCP(0),richiesta);
                    if(richiesta.getCodop() == 9){
                        int esito;
                        try{
                            esito = Utility.riceviDocumento(daServire, d,richiesta.getNumsezioni());
                        }catch (IOException ioe){
                            esito = -1;
                        }
                        Utility.sendResponse(daServire,new RispostaTCP(esito),richiesta);
                    }
                    if(lst != null){
                        String ntf = "Il documento: "+richiesta.getNomefile()+" sezione: "
                                +richiesta.getNumsezioni()+" è ora disponibile per la modifica.";
                        // Notifica a chi voleva editare il file solo se ancora online
                        for(String usr : lst)
                            Utility.sendNotification(daServire, usr, ntf, true);
                        // Cancellazione lista attesa edit
                        d.clearAttesa(richiesta.getNumsezioni());
                    }
                    // Stampa lato server
                    System.out.println("L'utente: "+richiesta.getUsername()+" ha terminato edit documento: "+
                            richiesta.getNomefile());
                    // Metodo per il riuso degli indirizzi multicast non più assegnati
                    TuringServer.reuseMulticastAddress(d);
                }else{
                    // L'utente in questione non stava modificando la sezione
                    resp = new RispostaTCP(-2);
                    // Stampa lato server
                    System.out.println("L'utente: " + richiesta.getUsername()+" non stava modificando " +
                            "la sezione su cui ha richiesto end-edit");
                }
                if(resp != null)
                    Utility.sendResponse(daServire, resp,richiesta);
                break;
            }
            default:{
                // Risposta di default con esito negativo (op non presenti/implementate)
                Utility.sendResponse(daServire, new RispostaTCP(-1),richiesta);
                break;
            }
        }
        try{
            daServire.register(TuringServer.serverSelector,SelectionKey.OP_READ);
        }catch (ClosedChannelException ignored){}
        mutex.unlock();
    }

}
