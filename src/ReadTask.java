import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.ReentrantLock;

public class ReadTask implements Runnable{
    private SocketChannel daServire;

    ReadTask(SocketChannel sc){
        daServire = sc;
    }

    public void run() {
        // Lock instanziata nel momento dell' accept
        ReentrantLock mutex = TuringServer.lockChannel.get(daServire);
        mutex.lock();
        // Operazioni per il recupero dell'operazione richiesta.
        ByteBuffer jsonletto = ByteBuffer.allocate(4);
        int sizeJson;
        try{
            sizeJson = Utility.readJson(daServire, jsonletto);
            if(sizeJson > 0){
                jsonletto = ByteBuffer.allocate( ((ByteBuffer)jsonletto.flip()).getInt() );
                sizeJson = Utility.readJson(daServire, jsonletto);
            }
        }catch (Exception e){
            System.err.println("Fallimento Lettura richiesta da socket");
            Utility.logout(daServire, null);
            Utility.closeChannel(daServire);
            mutex.unlock();
            TuringServer.lockChannel.remove(daServire,mutex);
            return;
        }
        if(sizeJson <= 0){
            Utility.logout(daServire, null);
            Utility.closeChannel(daServire);
            mutex.unlock();
            TuringServer.lockChannel.remove(daServire,mutex);
            return;
        }
        jsonletto.flip();
        // Riconverto il json
        JsonReader reader = new JsonReader(new StringReader(new String(jsonletto.array())));
        TuringServer.sockReq.put(daServire,(new Gson()).fromJson(reader, RichiestaTCP.class));
        mutex.unlock();
    }
}
