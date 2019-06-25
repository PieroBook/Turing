import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class UsersDocsHandler implements InterfaceUser {
    // Struttura dati già sincronizzata per gestire utenti
    private ConcurrentHashMap<String,Utente> registrati;
    // Strutture dati già sincronizzate per gestire file per ogni utente
    private ConcurrentHashMap<String, List<Documento>> userDoc;

    UsersDocsHandler(){
        registrati = new ConcurrentHashMap<>();
        userDoc = new ConcurrentHashMap<>();
    }

    // Metodo esposto via RMI
    public boolean registraUtente(String name,String surname,String username,String password) {
        if(registrati.containsKey(username)){
            return false;
        }
        // Creo directory per utente
        try {
            Files.createDirectories(Paths.get("DATA/"+username).toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        // Crea nuovo oggetto utente
        Utente nuovo = new Utente(name,surname,username,password);
        // Inserisce utente in Hashmap
        registrati.put(username,nuovo);
        // Crea la lista con cui associa utente a documenti
        userDoc.put(username, Collections.synchronizedList(new ArrayList<>()));
        System.out.println("Registrato nuovo utente: "+username);
        return true;
    }

    // Metodo non esposto esternamente ritorna 0 se corretto -1 se user non esistente ,1 se password errata, -2 gia loggato
    int login(String username,String pwd) {
        Utente n ;
        if( (n = registrati.get(username)) == null)
            return -1;
        if(n.isOnline())
            return -2;
        if( n.checkPassword(pwd) ) {
            n.setOnline();
            return 0;
        }
        return 1;
    }

    // Aggiunge documento per l'utente
    void addDoc(Utente u, Documento d){
         userDoc.get(u.getUsername()).add(d);
    }

    // Recupera documento di uno specifico utente
    Documento getDoc(Utente u,String nomeDoc, boolean owned ){
        for(Documento s: userDoc.get(u.getUsername())){
            if(s.getNomefile().compareTo(nomeDoc) == 0){
                if(s.getOwner().equals(u.getUsername()))
                    return s;
                else if(!owned)
                    return getDoc(registrati.get(s.getOwner()),nomeDoc,true);
            }
        }
        return null;
    }

    // Metodo logout utenti per shutdown server
    void setLogout(){
        for(String k : registrati.keySet()){
            registrati.get(k).setOffline();
        }
    }

    // Inizializza informazioni transient di ogni doumento e utente
    void initTransient(){
        for(List<Documento> lst : userDoc.values()){
            for (Documento d : lst){
                d.initTransient();
            }
        }
        for(Utente u: registrati.values())
            u.initTransient();
    }

    // Ritorna Struttura dati corrispondente all'username
    Utente getRegisteredUser(String username) {
        if(username == null) return null;
        return registrati.get(username);
    }

    // Ritorna lista di documenti per utente
    List<Documento> getUserDocuments(String username){
        return userDoc.get(username);
    }
}
