import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.apache.commons.codec.digest.DigestUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReentrantLock;

public class Turing extends Frame {
    // Socket di comunicazione
    static SocketChannel clientSocket;
    static ReentrantLock mutex;
    // Risorsa condivisa via RMI
    static InterfaceUser usr;
    // Nome utente logged-in
    static String currentUsername;
    // List informazioni
    private RispostaTCP lista;
    // Thread notifiche
    private static Thread updater;
    static boolean updateNeeded;
    // GraphicUserInterface stuffs
    private Label utentecorrente;
    private String nullField = "------------------";
    private TextField username;
    private Choice file_choice,section_choice;
    private Button add,list,mod,vis;

    // Costruttore artefatti grafici e loro eventListener
    private Turing(){
        // Setting Frame
        super("Turing - Gestione Documenti");
        setSize(500,300);
        setVisible(false);
        setResizable(false);
        setLayout(null);

        // Creo etichette testo
        Label namefile = new Label("Nome File");
        Label section = new Label("Sezione");
        Label coll = new Label("Collaboratori");
        Label nameuser = new Label("Nome Utente");
        // Creo textfield
        username = new TextField();
        username.setEnabled(false);
        // Creo Choice
        file_choice = new Choice();
        section_choice = new Choice();
        // Creo bottoni
        Button newfile = new Button("Nuovo File");
        Button logout = new Button("Logout");
        add = new Button("Aggiungi");
        list = new Button("Lista");
        add.setEnabled(false);
        list.setEnabled(false);
        mod = new Button("Modifica");
        vis = new Button("Visualizza");
        section_choice.setEnabled(false);
        mod.setEnabled(false);
        vis.setEnabled(false);

        // Setto posizione oggetti
        namefile.setBounds(25,40,100,20);
        section.setBounds(25,110,100,20);
        coll.setBounds(335,40,100,20);
        nameuser.setBounds(290,70,100,20);
        username.setBounds(290,100,170,20);
        file_choice.setBounds(25,60,200,30);
        section_choice.setBounds(25,130,200,30);
        newfile.setBounds(100,260,100,20);
        logout.setBounds(300,260,100,20);
        add.setBounds(275,160,90,20);
        list.setBounds(385,160,90,20);
        mod.setBounds(25,190,90,20);
        vis.setBounds(125,190,90,20);

        // Aggancio oggetti al frame
        add(username);
        add(nameuser);
        add(namefile);
        add(section);
        add(coll);
        add(file_choice);
        add(section_choice);
        add(newfile);
        add(logout);
        add(add);
        add(list);
        add(mod);
        add(vis);

        // Lista utenti per file
        List listaCondivisi = new List();
        listaCondivisi.setVisible(false);

        // Listener lista File
        file_choice.addItemListener(l-> {
            String file = this.file_choice.getSelectedItem();
            int notFilename;
            if((notFilename = file.indexOf(" - (")) != -1){
                file = file.substring(0,notFilename);
            }
            section_choice.removeAll();
            listaCondivisi.removeAll();
            // Nessun file disponibile, opzioni inibite
            if (file.compareTo(nullField) == 0 || lista == null){
                section_choice.setEnabled(false);
                add.setEnabled(false);
                list.setEnabled(false);
                mod.setEnabled(false);
                vis.setEnabled(false);
                username.setEnabled(false);
            } else{
                section_choice.setEnabled(true);
                int idx = file_choice.getSelectedIndex()-1;
                Documento d = lista.getFiles().get(idx);
                if(d.getNumsezioni() != 1){
                    section_choice.add("Tutte le sezioni");
                    mod.setEnabled(false);
                }
                else{
                    mod.setEnabled(true);
                }
                for (int j = 1; j <= d.getNumsezioni(); j++) {
                    section_choice.add(Integer.toString(j));
                }
                for(String str : d.getSharedWith()){
                    if(str.compareTo(currentUsername)!=0)
                        listaCondivisi.add(str);
                }
                if (d.getOwner().compareTo(currentUsername)==0) {
                    add.setEnabled(true);
                    username.setEnabled(true);
                    username.setText("");
                }else{
                    add.setEnabled(false);
                    username.setEnabled(false);
                }
                list.setEnabled(true);
                vis.setEnabled(true);
            }
        });

        // Listener lista Sezioni
        section_choice.addItemListener(l-> {
            // Se sceglie "tutte le sezioni" inibisco tasto di modifica
            if(section_choice.getSelectedItem().compareTo("Tutte le sezioni")==0)
                mod.setEnabled(false);
            else
                mod.setEnabled(true);
        });

        // Listener Frame Nuovo Documento
        newfile.addActionListener(e->{
            new NuovoFile(this);
            this.setVisible(false);
        });

        // Listener btn logout
        logout.addActionListener(e->this.dispose());

        // Listener btn addCondivisore
        add.addActionListener(e->{
            if(username.getText().isEmpty()){
                JOptionPane.showMessageDialog(this, "Inserisci username dell'utente.",
                        "Turing - Info",JOptionPane.INFORMATION_MESSAGE,
                        new ImageIcon("drawable/fault.png"));
            }else{
                if(username.getText().compareTo(currentUsername)==0){
                    JOptionPane.showMessageDialog(this, "Nome utente non valido.",
                            "Turing - Info",JOptionPane.INFORMATION_MESSAGE,
                            new ImageIcon("drawable/fault.png"));
                }else{
                    mutex.lock();
                    RispostaTCP response = requestAndReply(
                            new RichiestaTCP(5,currentUsername,file_choice.getSelectedItem(),username.getText()));
                    mutex.unlock();
                    if(response != null){
                        if(response.getEsito() == 0) {
                            JOptionPane.showMessageDialog(this, "Utente aggiunto correttamente.",
                                    "Turing - Info",JOptionPane.INFORMATION_MESSAGE,
                                    new ImageIcon("drawable/info.png"));
                            //aggiornaInfoClient();
                        }else if(response.getEsito() == -1)
                            JOptionPane.showMessageDialog(this, "Nome utente non esistente.",
                                    "Turing - Info",JOptionPane.INFORMATION_MESSAGE,
                                    new ImageIcon("drawable/fault.png"));
                        else if(response.getEsito() == 1)
                            JOptionPane.showMessageDialog(this, "Documento già condiviso con username indicato.",
                                    "Turing - Info",JOptionPane.INFORMATION_MESSAGE,
                                    new ImageIcon("drawable/fault.png"));
                    }else{
                        JOptionPane.showMessageDialog(this, "Problema di comunicazione con server.",
                                "Turing - Error",JOptionPane.ERROR_MESSAGE,
                                new ImageIcon("drawable/error.png"));
                    }
                }
            }
        });

        // Listener listaCondivisori
        list.addActionListener(e->{
            if(!listaCondivisi.isVisible()) {
                listaCondivisi.setLocation(275,70);
                listaCondivisi.setSize( 200, 110);
                remove(nameuser);
                remove(username);
                remove(add);
                listaCondivisi.setVisible(true);
                add(listaCondivisi);
                list.setLabel("Chiudi Lista");
                list.setBounds(325,190,100,20);
            }else{
                listaCondivisi.setVisible(false);
                remove(listaCondivisi);
                add(nameuser);
                add(username);
                add(add);
                list.setBounds(385,160,90,20);
                list.setLabel("Lista");
            }
        });

        // Listener btn visSezione
        vis.addActionListener(e->{
            String file = this.file_choice.getSelectedItem();
            int notFilename;
            if((notFilename = file.indexOf(" - (")) != -1){
                file = file.substring(0,notFilename);
            }
            int numsez = 0;
            if(section_choice.getSelectedItem().compareTo("Tutte le sezioni")!=0){
                numsez = Integer.parseInt(section_choice.getSelectedItem());
            }
            mutex.lock();
            RispostaTCP response = requestAndReply(
                    new RichiestaTCP(4,currentUsername,file,numsez));
            if(response == null){
                JOptionPane.showMessageDialog(this, "Problema di comunicazione con server.",
                        "Turing - Error",JOptionPane.ERROR_MESSAGE,
                        new ImageIcon("drawable/error.png"));
            }else{
                try{
                    if(section_choice.getItemCount() == 1)
                        numsez = 0;
                    riceviSezioneFile(file,numsez);
                }catch (IOException ignored){
                    JOptionPane.showMessageDialog(this, "Errore nel recupero del file!",
                            "Turing - Error",JOptionPane.ERROR_MESSAGE,
                            new ImageIcon("drawable/error.png"));
                    return;
                }
                JOptionPane.showMessageDialog(this, "File recuperato correttamente!",
                        "Turing - Info",JOptionPane.INFORMATION_MESSAGE,
                        new ImageIcon("drawable/cloud.png"));
            }
            mutex.unlock();
        });

        // Listener btn modSezione
        mod.addActionListener(e->{
            String file = this.file_choice.getSelectedItem();
            int notFilename;
            if((notFilename = file.indexOf(" - (")) != -1){
                file = file.substring(0,notFilename);
            }
            int numsez = Integer.parseInt(section_choice.getSelectedItem());
            mutex.lock();
            RispostaTCP response = requestAndReply(
                    new RichiestaTCP(3,currentUsername,file,numsez));
            if(response == null){
                JOptionPane.showMessageDialog(this, "Problema di comunicazione con server.",
                        "Turing - Error",JOptionPane.ERROR_MESSAGE,
                        new ImageIcon("drawable/error.png"));
            }else{
                if(response.getEsito() == 0){
                    String path;
                    String md5 = null;
                    int val = numsez;
                    if(section_choice.getItemCount() == 1)
                        val = 0;
                    try{
                        path = riceviSezioneFile(file,val);
                    }catch (IOException ignored){
                        JOptionPane.showMessageDialog(this, "Errore nel recupero del file!",
                                "Turing - Error",JOptionPane.ERROR_MESSAGE,
                                new ImageIcon("drawable/error.png"));
                        mutex.unlock();
                        return;
                    }
                    try (InputStream is = Files.newInputStream(Paths.get(path))) {
                        md5 = DigestUtils.md5Hex(is);
                    }catch (IOException ignored){}
                    this.setVisible(false);
                    new EditFrame(this, file, numsez, md5,response.getNotifiche().get(0));
                }else{
                    JOptionPane.showMessageDialog(this, response.getNotifiche().get(0),
                            "Turing - Info",JOptionPane.INFORMATION_MESSAGE,
                            new ImageIcon("drawable/docInfo.png"));
                }
            }
            mutex.unlock();
        });

        // Listener window
        addWindowListener(new WindowAdapter(){
            public void windowOpened(WindowEvent e) {
                (updater = new Thread(() -> {
                    updateNeeded = true;
                    while(!Thread.interrupted()){
                        aggiornaInfoClient();
                        try {
                            Thread.sleep(1750);
                        } catch (InterruptedException ignored) {}
                    }
                })).start();
            }
            public void windowClosing(WindowEvent e){
                e.getWindow().dispose();
            }
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        // Linee grafiche di separazione "zone" app
        g.drawLine(0,240,500,240);
        g.drawLine(250,0,250,240);
        g.drawLine(500,0,500,300);
    }

    public static void main(String[] args){
        // Creo lock per accesso a socket
        mutex = new ReentrantLock();
        // Avvio GUI
        // In caso di terminazione forzata/imprevista
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Chiusura socket, logout e interruzione updater
            if(currentUsername != null){
                updater.interrupt();
                System.err.println("Procedo alla chiusura del client");
                try{
                    clientSocket.close();
                }catch(Exception ignored){}
            }
            System.err.println("Chiusura Client:: Terminato.");
        }));
        new Login(new Turing()).setVisible(true);
        System.out.println("Client Pronto!");
    }

    static RispostaTCP login(String user, String passw){
        // Creo SocketChannel
        try {
            Turing.clientSocket = SocketChannel.open();
            Turing.clientSocket.connect(new InetSocketAddress(InetAddress.getLocalHost(),11223));
            clientSocket.configureBlocking(false);
        } catch (Exception e1) {
            try {
                Turing.clientSocket.close();
            } catch (IOException ignored) {
                System.err.println("Socket Non aperto e non chiuso");
                return null;
            }
        }
        RichiestaTCP login = new RichiestaTCP(1,user,passw);
        mutex.lock();
        RispostaTCP responso = requestAndReply(login);
        mutex.unlock();
        return responso;
    }

    static RispostaTCP requestAndReply(RichiestaTCP richiesta){
        // Converto json in byte
        byte [] byte_richiesta = new Gson().toJson(richiesta).getBytes();
        // Alloca ByteBuffer per contenere richiesta
        ByteBuffer buffer = (ByteBuffer) ByteBuffer.allocate(byte_richiesta.length).put(byte_richiesta).flip();
        ByteBuffer len = (ByteBuffer) ByteBuffer.allocate(4).putInt(byte_richiesta.length).flip();
        // Scrive su socketChannel
        try {
            while(len.hasRemaining())
                clientSocket.write(len);
            while(buffer.hasRemaining())
                clientSocket.write(buffer);
        } catch (IOException ioe) {
            return null;
        }
        buffer.clear();
        // Leggo la risposta
        RispostaTCP response = getReply(true);
        if(response != null) {
            try {
                // Gestisco caso di login fallito e logout
                if ((richiesta.getCodop() == 1 && response.getEsito() != 0) || richiesta.getCodop() == 8) {
                    clientSocket.close();
                    currentUsername = null;
                }
            } catch (IOException ignored) {
                System.err.println("Errore logout:: Chiuso");
            }
        }
        return response;
    }

    static RispostaTCP getReply(boolean waitUntilResponse){
        RispostaTCP response = new RispostaTCP(-1);
        ByteBuffer dim = ByteBuffer.allocate(4);
        int letto = 0;
        try {
            do{
                letto += clientSocket.read(dim);
            }while(letto < 4 && waitUntilResponse);
        } catch (Exception e) {
            return response;
        }
        if(letto == -1)
            return response;
        if(letto == 0)
            return null;
        int val = ((ByteBuffer)dim.flip()).getInt();
        // Alloco Buffer da dim byte
        ByteBuffer buffer = ByteBuffer.allocate(val);
        int letto2 = 0;
        // Leggo risposta
        try {
            do{
                letto2 += clientSocket.read(buffer);
            }while(letto2 < letto);
        } catch (Exception e) {
            return response;
        }
        if (letto <= 0){
            return response;
        }
        // Riconverto il json
        String jsonString = new String(((ByteBuffer)buffer.flip()).array()).trim();
        JsonReader reader = new JsonReader(new StringReader(jsonString));
        try{
            response = (new Gson()).fromJson(reader, RispostaTCP.class);
        }catch (com.google.gson.JsonSyntaxException ex){
            return response;
        }
        return response;
    }

    private String riceviSezioneFile(String name, int sez) throws IOException{
        // Divide nome del documento in nomefile ed estensione
        String[] split = name.split("\\.");
        // Aggiunge alla stringa il path utente
        String path = currentUsername+"_Docs/"+split[0]+"_"+sez+"."+split[1];
        if (sez == 0)
            path = currentUsername+"_Docs/"+name;
        Path filepath = Paths.get(path).toAbsolutePath();
        // Crea FileChannel in ricezione
        FileChannel incoming = FileChannel.open(filepath, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
        ByteBuffer len = ByteBuffer.allocate(8);
        // Legge la dimensione attesa
        long letto = 0;
        do{
            letto += clientSocket.read(len);
        }while(letto < 8);
        long incomingSize = ((ByteBuffer)len.flip()).getLong();
        letto = 0;
        // Avvia trasferimento documento via filechannel
        while ((letto += incoming.transferFrom(clientSocket,letto,incomingSize))!= incomingSize);
        incoming.close();
        return filepath.toString();
    }

    private void aggiornaInfoClient(){
        mutex.lock();
        // Invia richiesta lista info
        RispostaTCP nlista;
        if(updateNeeded)
            nlista = requestAndReply(new RichiestaTCP(7,currentUsername));
        else
            nlista = getReply(false);
        mutex.unlock();
        // Se richiesta a buon fine aggiorno tutte le info da visualizzare
        if(nlista!=null){
            // Server non raggiungibile
            if(nlista.getEsito() == -1)
                crashExit();
            lista = nlista;
            int file = file_choice.getSelectedIndex();
            int section = section_choice.getSelectedIndex();
            file_choice.removeAll();
            section_choice.removeAll();
            file_choice.add(nullField);

            if(lista.getFiles()!= null && !lista.getFiles().isEmpty()){
                for(Documento n : lista.getFiles()){
                    if(n.getOwner().compareTo(currentUsername)!=0)
                        file_choice.add(n.getNomefile()+" - ("+n.getOwner()+")");
                    else
                        file_choice.add(n.getNomefile());
                }
            }
            if(file!= -1 && section != -1) {
                file_choice.select(file);
                file_choice.getItemListeners()[0].itemStateChanged(
                        new ItemEvent(file_choice, ItemEvent.ITEM_STATE_CHANGED, file_choice, ItemEvent.SELECTED));
                section_choice.select(section);
                section_choice.getItemListeners()[0].itemStateChanged(
                        new ItemEvent(section_choice, ItemEvent.ITEM_STATE_CHANGED, section_choice, ItemEvent.SELECTED));
            }
            // Mostro notifiche pendenti
            if(lista.getNotifiche()!=null && !lista.getNotifiche().isEmpty() &&
                    lista.getNotifiche().get(0) != null){
                StringBuilder strb = new StringBuilder();
                for(String s : lista.getNotifiche()){
                    strb.append(s).append("\n");
                }
                JOptionPane.showMessageDialog(this,strb.toString(),
                        "Turing - Notifiche",JOptionPane.INFORMATION_MESSAGE,
                        new ImageIcon("drawable/docinfo.png"));
            }
            updateNeeded = false;
        }
        // Dopo login imposto il nome utente in basso a dx nel Frame Turing
        if(utentecorrente == null){
            utentecorrente = new Label(currentUsername);
            utentecorrente.setBounds(400,220,100,20);
            utentecorrente.setAlignment(Label.CENTER);
            utentecorrente.setFont(new Font("",Font.PLAIN,10));
            add(utentecorrente);
        }
    }

    private void crashExit(){
        currentUsername = null;
        String msg = "Il server non è disponibile. Chiusura app";
        JOptionPane.showMessageDialog(this.getFocusOwner(),msg,
                "Turing - Server Crash",JOptionPane.ERROR_MESSAGE,
                new ImageIcon("drawable/exit.png"));
        System.exit(1);
    }
}