import org.apache.commons.codec.digest.DigestUtils;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

class EditFrame extends Frame {
    private String sendBackFile;
    private String nomeDocumento;
    private String currentPath;
    private String hashFile;
    Chat chatArea;
    private MulticastChat mc;
    private int sezione;

    EditFrame(Frame c, String nomeDocumento, int sezione, String hashfile, String addressGroup){
        super("Turing - Edit Documenti");
        setSize(700,450);
        setLocation(c.getLocation());
        setVisible(Boolean.TRUE);
        setResizable(Boolean.FALSE);
        setLayout(null);
        // Var. istanza
        this.currentPath = Paths.get(Turing.currentUsername+"_Docs").toAbsolutePath().toString();
        this.nomeDocumento = nomeDocumento;
        this.sezione = sezione;
        this.hashFile = hashfile;
        this.mc = new MulticastChat(this,addressGroup);
        // Oggetti grafici
        Label namefile = new Label("Nome File");
        Label section = new Label("Numero Sezione");
        Label percorso = new Label("Path File");
        Label labelChat = new Label("Chat");
        labelChat.setBounds(490,25,70,20);
        add(labelChat);
        TextArea corpoChat = new TextArea("",3,30,TextArea.SCROLLBARS_VERTICAL_ONLY);
        Button inviaChat = new Button("Invia");
        namefile.setBounds(20,40,120,20);
        section.setBounds(20,70,120,20);
        percorso.setBounds(20,100,120,20);
        corpoChat.setBounds(20,220,320,120);
        inviaChat.setBounds(120,370,120,20);
        add(namefile);
        add(section);
        add(percorso);
        chatArea = new Chat();
        chatArea.setVisible(true);
        chatArea.setLocation(360,50);
        add(chatArea);
        add(corpoChat);
        add(inviaChat);
        Label filecorrente = new Label(nomeDocumento);
        Label numsezione = new Label("Sezione "+sezione);
        Label filescelto = new Label("Scegli file da caricare");
        filescelto.setForeground(Color.blue);
        filecorrente.setBounds(180,40,150,20);
        numsezione.setBounds(180,70,150,20);
        filescelto.setBounds(180,100,150,20);
        add(numsezione);
        add(filecorrente);
        add(filescelto);
        Button end_edit = new Button("End Editing");
        Button undo = new Button("Annulla");
        end_edit.setBounds(30,140,130,20);
        undo.setBounds(190,140,130,20);
        end_edit.setEnabled(Boolean.FALSE);
        add(end_edit);
        add(undo);

        Thread chatHandler = new Thread( ()->{
            mc.run();
            while (!Thread.interrupted());
        });
        chatHandler.start();

        end_edit.addActionListener(e-> {
            // Verifica modifiche effettive e procede all'invio della nuova versione
            int res = sendModification();
            if(res == -1){
                JOptionPane.showMessageDialog(this, "Editing fallito. Ritentare",
                        "Turing - Fail", JOptionPane.INFORMATION_MESSAGE,
                        new ImageIcon("drawable/error.png"));
                return;
            }
            if(res == 0){
                // Comunicazione finale
                JOptionPane.showMessageDialog(this, "Editing concluso. Il nuovo documento\n" +
                "è stato inviato al Server", "Turing - Info", JOptionPane.INFORMATION_MESSAGE,
                new ImageIcon("drawable/cloud.png"));
            }
            c.setVisible(Boolean.TRUE);
            chatHandler.interrupt();
            this.dispose();
        });

        undo.addActionListener(e->{
            undoModification();
            c.setVisible(Boolean.TRUE);
            chatHandler.interrupt();
            this.dispose();
        });

        filescelto.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                JFileChooser chooser = new JFileChooser(currentPath);
                chooser.setFileFilter( new FileNameExtensionFilter("Text File", "txt"));
                int returnVal = chooser.showOpenDialog(e.getComponent());
                if(returnVal == JFileChooser.APPROVE_OPTION) {
                    sendBackFile = chooser.getSelectedFile().getName();
                    filescelto.setText("<html><u>"+sendBackFile+"</u></html>");
                    end_edit.setEnabled(Boolean.TRUE);
                }
            }
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });

        inviaChat.addActionListener(e -> {
            String testo = corpoChat.getText().trim();
            if(testo.length() != 0){
                mc.sendMessage(Turing.currentUsername+": "+testo+"\n");
                corpoChat.setText("");
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        // Linee grafiche di separazione "zona file" app
        g.drawLine(0,190,350,190);
        g.drawLine(350,0,350,190);
        // Linne grafiche di separazione "chat"
        g.drawLine(359,49,689,49);
        g.drawLine(359,49,359,425);
        g.drawLine(359,425,689,425);
    }

    private int sendModification(){
        int value = -1;
        if(sendBackFile != null){
            String newhash = "";
            Path filePath = Paths.get(currentPath+"/"+sendBackFile).toAbsolutePath();
            //Calcola CheckSum della sezione in questione
            try (InputStream is = Files.newInputStream(Paths.get(filePath.toString()))) {
                newhash = DigestUtils.md5Hex(is);
            }catch (IOException ignored){}
            // Se la sezione ha lo stesso hash non reinvia indietro il file.
            if(this.hashFile.equals(newhash)){
                // END_EDIT senza modifica
                undoModification();
                return 1;
            }
            Turing.mutex.lock();
            FileChannel tmp_documento;
            ByteBuffer len;
            try {
                // Invia richiesta di end-edit con modifica
                Turing.requestAndReply(
                        new RichiestaTCP(9,Turing.currentUsername,nomeDocumento,sezione));
                // Sezione da inviare
                tmp_documento = FileChannel.open(filePath, StandardOpenOption.READ);
                // Lunghezza della sezione
                len = (ByteBuffer) ByteBuffer.allocate(8).putLong(tmp_documento.size()).flip();
                // Invio size nuova sez doc
                Turing.clientSocket.write(len);
                // Invio nuova sez doc
                long tmp = tmp_documento.size();
                while ((tmp -= tmp_documento.transferTo(0, tmp_documento.size(),Turing.clientSocket)) != 0 );
                tmp_documento.close();
                // Riceve info sull' esito della ricezione
                value = Turing.getReply().getEsito();
            }catch(IOException ioe){
                ioe.printStackTrace();
                System.err.println("Problema nella ritrasmissione info");
            }
            finally {
                Turing.mutex.unlock();
            }
        }
        return value;
    }

    private void undoModification(){
        // END-EDIT senza modifica
        Turing.mutex.lock();
        Turing.requestAndReply(
                new RichiestaTCP(10,Turing.currentUsername,nomeDocumento,sezione));
        Turing.mutex.unlock();
        JOptionPane.showMessageDialog(this, "Editing concluso. Nessuna modifica\n" +
                        "è stata inviata al Server","Turing - Info",JOptionPane.INFORMATION_MESSAGE,
                new ImageIcon("drawable/info.png"));
    }
}
