import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;

class Registrazione extends Frame {
    // Thread RMI
    private static Thread rmiInstauration;
    private Frame padre;
    private Button conferma,annulla;
    private TextField nome,cognome,nomeutente,password;
    private Checkbox chiaro;

    Registrazione(Login l){
        // Setting Frame
        super("Turing - Registrazione");
        setSize(420,200);
        setLocation(l.getLocation());
        setVisible(true);
        setResizable(Boolean.FALSE);
        setLayout(null);
        // Frame padre
        this.padre = l;
        // Creo label
        Label name = new Label("Nome");
        Label surname = new Label("Cognome");
        Label username = new Label("Username");
        Label psw = new Label("Password");
        // Creo bottoni
        conferma = new Button("Conferma");
        annulla = new Button("Annulla");
        // Creo TextField
        nome = new TextField();
        cognome = new TextField();
        nomeutente = new TextField();
        password = new TextField();
        password.setEchoChar('*');
        // CheckBox password
        chiaro = new Checkbox("Mostra Password",Boolean.FALSE);

        // Posiziono e dimensiono gli oggetti
        name.setBounds(35,50,130,20);
        surname.setBounds(35,70,130,20);
        username.setBounds(35,90,130,20);
        psw.setBounds(35,110,130,20);
        conferma.setBounds(35,170,95,20);
        annulla.setBounds(290,170,95,20);
        nome.setBounds(172,50,170,20);
        cognome.setBounds(172,70,170,20);
        nomeutente.setBounds(172,90,170,20);
        password.setBounds(172,110,170,20);
        chiaro.setBounds(172,130,200,30);

        // Aggancio gli oggetti al frame
        add(name);
        add(surname);
        add(username);
        add(psw);
        add(conferma);
        add(annulla);
        add(nome);
        add(cognome);
        add(nomeutente);
        add(password);
        add(chiaro);

        // --- Events Listeners --- //
        setupEventsListeners();

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e){
                e.getWindow().dispose();
            }
            public void windowClosed(WindowEvent e) {
                // Apro finistra Registrazione
                //Chiusura Thread RMI
                rmiInstauration.interrupt();
                l.setVisible(Boolean.TRUE);
            }
        });

        // Avvio instaurazione RMI
        // Avvio RMI per registrazione
        System.out.println("Avvio recupero RMI");
        (rmiInstauration = new Thread(new RmiClientInstauration())).start();
        try {
            rmiInstauration.join();
        } catch (InterruptedException ignored){}
        System.out.println("Connessione RMI Stabilita");
    }

    private void setupEventsListeners(){
        // Event Listener show pass char
        chiaro.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                password.setEchoChar('*');
            } else {
                password.setEchoChar((char) 0);
            }
        });

        // Event Listener Btn Conferma
        conferma.addActionListener(e -> {
            if(!nome.getText().isEmpty() && !nomeutente.getText().isEmpty() &&
                    !cognome.getText().isEmpty() && !password.getText().isEmpty()){
                // Invio e conferma registrazione se ok
                Boolean esito = null;
                try {
                    esito = Turing.usr.registraUtente(nome.getText(),cognome.getText(),nomeutente.getText().toLowerCase(),password.getText());
                } catch (RemoteException | NullPointerException e1) {
                    JOptionPane.showMessageDialog(this,"Errore in comunicazione con server.",
                            "Turing - Error",JOptionPane.ERROR_MESSAGE,
                            new ImageIcon("drawable/error.png"));
                }
                if(esito != null && esito){
                    // Registrazione riuscita
                    JOptionPane.showMessageDialog(this,"Registrazione completata con successo.",
                            "Turing - Success",JOptionPane.INFORMATION_MESSAGE,
                            new ImageIcon("drawable/cloud.png"));
                    padre.setVisible(Boolean.TRUE);
                    this.dispose();
                }else if(esito!=null){
                    //Se username gia presente
                    JOptionPane.showMessageDialog(this,"Registrazione Fallita:\nUsername già presente",
                            "Turing - Error",JOptionPane.INFORMATION_MESSAGE,
                            new ImageIcon("drawable/fault.png"));
                }
            }
        });

        // Event Listener btn UNDO
        annulla.addActionListener(e -> {
            // Chiudi finistra
            padre.setVisible(Boolean.TRUE);
            this.dispose();
        });
    }
}