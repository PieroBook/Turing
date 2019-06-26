import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

class NuovoFile extends Frame {
    // Frame padre
    private Frame padre;
    private Button ok,undo;
    private TextField nomefile,numsez;
    NuovoFile(Turing c){
        // Setting Frame
        super("Turing - Nuovo Documento");
        setSize(420,140);
        setLocation(c.getLocation());
        setVisible(Boolean.TRUE);
        setResizable(Boolean.FALSE);
        setLayout(null);
        // Inizializzo Frame Padre
        this.padre = c;
        // Creo label
        Label filename = new Label("Nome File");
        Label nsect = new Label("Numero Sezioni");
        // Creo bottoni
        ok = new Button("OK");
        undo = new Button("Annulla");
        // Caselle testo
        nomefile = new TextField();
        numsez = new TextField();

        // Setto posizione oggetti
        filename.setBounds(25,50,100,20);
        nsect.setBounds(25,90,100,20);
        ok.setBounds(305,50,80,20);
        undo.setBounds(305,90,80,20);
        nomefile.setBounds(150,50,120,20);
        numsez.setBounds(150,90,120,20);

        // Aggancio oggetti al frame
        add(filename);
        add(nsect);
        add(ok);
        add(undo);
        add(nomefile);
        add(numsez);

        //------ Event Listener ---- //
        setupEventsListeners();

        // Listener Window
        addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
                e.getWindow().dispose();
            }
            public void windowClosed(WindowEvent e) {
                // Apro finistra Turing e richiedo update info
                Turing.updateNeeded = true;
                padre.setVisible(Boolean.TRUE);
            }
        });
    }

    private void setupEventsListeners(){
        // Listener btn annulla
        undo.addActionListener(e -> {
            // Chiudi finistra
            this.dispose();
        });

        // Listener btn ok
        ok.addActionListener(e -> {
            String nomefileval = nomefile.getText();
            if(!nomefileval.isEmpty() && !numsez.getText().isEmpty()){
                int val;
                try{
                    val = Integer.parseInt(numsez.getText());
                }catch(NumberFormatException nfe){
                    JOptionPane.showMessageDialog(this,"Il campo Numero Sezioni deve essere \nun intero maggiore di 0",
                            "Turing - Error",JOptionPane.INFORMATION_MESSAGE,
                            new ImageIcon("drawable/fault.png"));
                    return;
                }
                if(val <= 0)
                    JOptionPane.showMessageDialog(this,"Il campo Numero Sezioni deve essere \nun intero maggiore di 0",
                            "Turing - Error",JOptionPane.INFORMATION_MESSAGE,
                            new ImageIcon("drawable/fault.png"));
                else if(!nomefileval.contains(".") || !((nomefileval.indexOf(".")+1)< nomefileval.length())){
                    JOptionPane.showMessageDialog(this,"Filename non valido",
                            "Turing - Error",JOptionPane.INFORMATION_MESSAGE,
                            new ImageIcon("drawable/fault.png"));
                }else {
                    // Chiede a Server creazione file
                    Turing.mutex.lock();
                    RispostaTCP response = Turing.requestAndReply(
                            new RichiestaTCP(2, Turing.currentUsername, nomefile.getText(), val));
                    Turing.mutex.unlock();
                    if (response != null){
                        if (response.getEsito() == 0) {
                            JOptionPane.showMessageDialog(this, "Creazione file avvenuta con successo.",
                                    "Turing - Error",JOptionPane.INFORMATION_MESSAGE,
                                    new ImageIcon("drawable/save.png"));
                            this.dispose();
                        } else if (response.getEsito() == -1) {
                            JOptionPane.showMessageDialog(this, "File già presente sul Server",
                                    "Turing - Error",JOptionPane.INFORMATION_MESSAGE,
                                    new ImageIcon("drawable/fault.png"));
                        } else{
                            JOptionPane.showMessageDialog(this, "Errore creazione file.",
                                    "Turing - Error",JOptionPane.INFORMATION_MESSAGE,
                                    new ImageIcon("drawable/fault.png"));
                        }
                    }else{
                        JOptionPane.showMessageDialog(this, "Problema comunicazione Server",
                                "Turing - Error",JOptionPane.ERROR_MESSAGE,
                                new ImageIcon("drawable/error.png"));
                    }
                }
            }
        });
    }
}
