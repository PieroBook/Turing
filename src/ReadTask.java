import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.ReentrantLock;

public class ReadTask implements Runnable{
    private SocketChannel daServire;
    private ReentrantLock mutex;

    ReadTask(SocketChannel sc){
        daServire = sc;
    }

    public void run() {
        // Lock instanziata nel momento dell' accept
        mutex = TuringServer.lockChannel.get(daServire);
        mutex.lock();
        // Operazioni per il recupero dell'operazione richiesta.
        ByteBuffer jsonletto = ByteBuffer.allocate(4);
        int sizeJson = 0;
        try{
            while (jsonletto.hasRemaining()){
                sizeJson = Utility.readJson(daServire, jsonletto);
                if(sizeJson == 4){
                    jsonletto = ByteBuffer.allocate( ((ByteBuffer)jsonletto.flip()).getInt() );
                    sizeJson = Utility.readJson(daServire, jsonletto);
                } else if(sizeJson == -1) {
                    handleExit();
                    return;
                }
            }
        }catch (Exception e){
            System.err.println("Fallimento Lettura richiesta da socket");
            handleExit();
            return;
        }
        jsonletto.flip();
        // Riconverto il json
        JsonReader reader = new JsonReader(new StringReader(new String(jsonletto.array())));
        TuringServer.sockReq.put(daServire,(new Gson()).fromJson(reader, RichiestaTCP.class));
        mutex.unlock();
    }

    private void handleExit(){
        Utility.logout(daServire, null);
        Utility.closeChannel(daServire);
        mutex.unlock();
        TuringServer.lockChannel.remove(daServire,mutex);
    }
}
