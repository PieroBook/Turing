import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class Login extends Frame {
    // Login Owner
    private Turing padre;
    // Oggeti di istanza
    private Button ok,register,exit;
    private TextField username,password;
    private Checkbox chiaro;

    Login(Turing c){
        // Setting Frame
        super("Turing - Login");
        setSize(420,200);
        setLocation(400,175);
        setResizable(false);
        setLayout(null);
        // Inizializzo Frame Padre
        this.padre = c;
        // Creo etichette testo
        Label usr = new Label("Nome Utente");
        Label psw = new Label("Password");
        // Creo bottoni
        ok = new Button("OK");
        register = new Button("Registra");
        exit = new Button("Esci");
        // Creo Input dati
        username = new TextField();
        password = new TextField("");
        password.setEchoChar('*');
        // Creo CheckBox password
        chiaro = new Checkbox("Mostra Password",Boolean.FALSE);

        // Setto posizione oggetti
        usr.setBounds(35,50,130,20);
        psw.setBounds(35,90,130,20);
        ok.setBounds(35,170,75,20);
        register.setBounds(172,170,75,20);
        exit.setBounds(310,170,75,20);
        username.setBounds(172,50,170,20);
        password.setBounds(172,90,170,20);
        chiaro.setBounds(172,110,200,30);

        // Aggancio oggetti al frame
        add(usr);
        add(psw);
        add(ok);
        add(register);
        add(exit);
        add(username);
        add(password);
        add(chiaro);

        //------ Event Listener ---- //
        setupEventsListeners();

        // Listener Finestra
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e){
                e.getWindow().dispose();
            }
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    private void setupEventsListeners(){
        // Lisener show passw char
        chiaro.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                password.setEchoChar('*');
            } else {
                password.setEchoChar((char) 0);
            }
        });

        // Listener Ok Button
        ok.addActionListener(e->{
            if((!username.getText().isEmpty() && !password.getText().isEmpty())) {
                RispostaTCP response = Turing.login(username.getText().toLowerCase(), password.getText());
                if (response == null) {
                    JOptionPane.showMessageDialog(this, "Errore in apertura comunicazione con server.",
                            "Turing - Error",JOptionPane.ERROR_MESSAGE,
                            new ImageIcon("drawable/error.png"));
                    return;
                }
                int val = response.getEsito();
                if (val == 0) {
                    Turing.currentUsername = username.getText().toLowerCase();
                    try {
                        Files.createDirectories(Paths.get(username.getText() + "_Docs/").toAbsolutePath());
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                    JOptionPane.showMessageDialog(this, "Login eseguito con successo.",
                            "Turing - Info",JOptionPane.INFORMATION_MESSAGE,
                            new ImageIcon("drawable/cloud.png"));
                    padre.setLocation(this.getLocation());
                    padre.setVisible(Boolean.TRUE);
                    this.setVisible(Boolean.FALSE);
                } else if (val == 1) {
                    JOptionPane.showMessageDialog(this, "Password errata.",
                            "Turing - Error",JOptionPane.INFORMATION_MESSAGE,
                            new ImageIcon("drawable/fault.png"));
                } else if (val == -1) {
                    JOptionPane.showMessageDialog(this, "Utente non esistente.",
                            "Turing - Error",JOptionPane.INFORMATION_MESSAGE,
                            new ImageIcon("drawable/fault.png"));
                } else if (val == -2) {
                    JOptionPane.showMessageDialog(this, "Utente già loggato.",
                            "Turing - Error",JOptionPane.INFORMATION_MESSAGE,
                            new ImageIcon("drawable/fault.png"));
                }
            }
        });

        // Listener register button
        register.addActionListener(e->{
            this.setVisible(Boolean.FALSE);
            new Registrazione(this);
        });

        // Listener Botone Uscita
        exit.addActionListener(e-> this.dispose());
    }
}