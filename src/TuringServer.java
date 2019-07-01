import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;

public class TuringServer {
    // IP Server and RMI
    private static final String ServerIP = "127.0.0.1";
    // Istanze condivise
    // Struttura Dati gestione utenti con metodo di registrazione esposto via RMI
    static UsersDocsHandler usersHandler;
    // Selettore per gestire Channel
    static Selector serverSelector;
    // Online user
    static Map<String,SocketChannel> online;
    // Mutex su channel
    static Map<SocketChannel, ReentrantLock> lockChannel;
    // Mappa socket - richiesta
    static Map<SocketChannel, RichiestaTCP> sockReq;
    // Mappa documento - multicastAddr
    static Map<Documento, String> docAddress;
    // Indirizzi Multicast
    static ConcurrentLinkedDeque<Integer> stackOne;

    public static void main(String[] args) throws IOException{
        // Recupero evantuali HashMap salvate precedentemente
        FileChannel letturaHM;
        try {
            letturaHM = FileChannel.open(Paths.get("DATA/HashMap.json").toAbsolutePath(),
                    StandardOpenOption.READ);
        }catch (IllegalArgumentException | UnsupportedOperationException | SecurityException | IOException e){
            letturaHM = null;
            usersHandler = new UsersDocsHandler();
        }
        // Inizializzazione HashMap e UsersDocsHandler
        if(letturaHM != null){
            StringBuilder letto = new StringBuilder();
            // Buffer da 128 byte
            ByteBuffer buf = ByteBuffer.allocate(128);
            while (letturaHM.read(buf) != -1) {
                buf.flip();
                String appenaletto = new String(buf.array());
                letto.append(appenaletto.trim());
                buf.clear();
            }
            JsonReader reader = new JsonReader(new StringReader(letto.toString()));
            reader.setLenient(true);
            usersHandler = (new Gson()).fromJson(reader, UsersDocsHandler.class);
            usersHandler.initTransient();
        }
        // Espongo Gestione registrazione via RMI
        try{
            InterfaceUser stub = (InterfaceUser) UnicastRemoteObject.exportObject(usersHandler, Registry.REGISTRY_PORT);
            Registry r = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
            r.rebind("Server-Turing", stub);
        }
        catch (RemoteException e) {
            System.err.println("Communication error " + e.toString());
        }
        // Instanzio ThreadPoolExecutor
        ThreadPoolExecutor executor=(ThreadPoolExecutor) Executors.newCachedThreadPool();
        // LISTENER :: Mi preparo a gestire richieste TCP
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(InetAddress.getByName(ServerIP),11223));
        serverSocket.configureBlocking(false);
        // Registro la serverSocket sul selector solo in accept
        serverSelector = Selector.open();
        serverSocket.register(serverSelector, SelectionKey.OP_ACCEPT);
        // Utenti online
        online = new ConcurrentHashMap<>();
        // Mappa di lock
        lockChannel = new ConcurrentHashMap<>();
        // Mappa di richieste
        sockReq = new ConcurrentHashMap<>();
        // Mappa di indirizzi multicast
        docAddress = new ConcurrentHashMap<>();
        System.err.println("Server Avviato Correttamente:: CTRL+C per Terminare");
        // Inizializzo stack di indirizzi
        stackOne = new ConcurrentLinkedDeque<>();
        for(int i=1; i<255;i++){
            stackOne.addLast(i);
        }
        System.out.println("Server Local Addr: "+serverSocket.socket());
        // Svolge funzione di Listener
        while(!Thread.interrupted()){
            SocketChannel daServire;
            serverSelector.select();
            Iterator it = serverSelector.selectedKeys().iterator();
            while(it.hasNext()) {
                SelectionKey k = (SelectionKey) it.next();
                it.remove();
                if (k.isValid() && k.isAcceptable()) {
                    daServire = serverSocket.accept();
                    lockChannel.put(daServire,new ReentrantLock());
                    daServire.configureBlocking(Boolean.FALSE);
                    daServire.register(serverSelector,SelectionKey.OP_READ );
                }
                else if (k.isValid() && k.isReadable()){
                    daServire = (SocketChannel) k.channel();
                    System.err.println("Leggo Richiesta di: "+daServire);
                    executor.execute(new ReadTask(daServire));
                    daServire.register(serverSelector, SelectionKey.OP_WRITE);
                }
                else if (k.isValid() && k.isWritable()){
                    daServire = (SocketChannel) k.channel();
                    RichiestaTCP richiesta = sockReq.get(daServire);
                    if(richiesta != null){
                        System.err.println("Elaboro e Rispondo richiesta di: "+daServire);
                        executor.execute(new WriteTask(daServire,richiesta));
                        sockReq.remove(daServire);
                    }
                }
            }
            // Solo un Signal puo' interrompere l'esecuzione
        }

        // Handler Terminazione server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            usersHandler.setLogout();
            System.err.println("Chiusura Server:: Conservo HashMap.");
            // Conservo HT Utenti
            FileChannel scritturaHT;
            try {
                scritturaHT = FileChannel.open(Paths.get("DATA/HashMap.json").toAbsolutePath(),
                        StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            // Crea un stringa json dell'oggetto e lo converte in un array di byte
            byte [] input = (new GsonBuilder().create()).toJson(usersHandler).getBytes();
            // Alloca un buffer per contenere esattamente l'input
            ByteBuffer buffer = ByteBuffer.allocateDirect(input.length);
            // inserisce nel buffer
            buffer.put(input);
            // passa in modalità lettura
            buffer.flip();
            try {
                while(buffer.hasRemaining())
                    scritturaHT.write(buffer);
            } catch (IOException e) {
                // Errore in Scrittura
                e.printStackTrace();
            }
            finally {
                try {
                    scritturaHT.close();
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.err.println("Chiusura Server:: Attendo fine ThreadPoolExecutor");
            // Termino ThreadPoolExecutor
            executor.shutdown();
            System.err.println("Chiusura Server:: Terminato.");
        }));
    }
}
