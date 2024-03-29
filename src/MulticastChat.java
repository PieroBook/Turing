import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class MulticastChat implements Runnable {
    private EditFrame frameEdit;
    private InetAddress multicastGroup;
    private DatagramSocket ds;

    MulticastChat(EditFrame ef,String address){
        frameEdit = ef;
        try {
            this.multicastGroup= InetAddress.getByName(address);
            ds = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // Task ricezione messaggi in chat
        try(MulticastSocket socket = new MulticastSocket(3000)){
            int LENGTH = 512;
            DatagramPacket packet = new DatagramPacket(new byte[LENGTH], LENGTH);
            socket.setSoTimeout(100000000);
            socket.joinGroup(multicastGroup);
            while(!Thread.interrupted()){
                socket.receive(packet);
                String str = new String(packet.getData(),packet.getOffset(),packet.getLength(), StandardCharsets.UTF_8);
                String[] vett = str.split("[:]");
                frameEdit.chatArea.addMsg(
                        vett[0].equals(Turing.currentUsername), str);
            }
        } catch (Exception e) {
	    frameEdit.chatArea.setEnabled(false);
	    frameEdit.inviaChat.setEnabled(false);
            System.err.println("Nessuna interfaccia di rete attiva. Solo localhost");
        }
    }

    // Metodo per l'invio di un messaggio ad un gruppo multicast
    void sendMessage(String msg){
        try{
            DatagramPacket packet = new DatagramPacket(
                    msg.getBytes(StandardCharsets.UTF_8),0,
                    msg.getBytes(StandardCharsets.UTF_8).length,
                    multicastGroup,3000);
            ds.send(packet);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
