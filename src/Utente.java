import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Utente {
    private String username;
    private String nome;
    private String cognome;
    private String password;
    private List<String> notifiche;
    transient private boolean online;

    Utente(String n, String c, String u, String pwd) {
        this.nome = n;
        this.cognome = c;
        this.username = u;
        this.password = DigestUtils.md5Hex(pwd);
        this.notifiche = Collections.synchronizedList(new ArrayList<>());
    }

    // Gestione online/offile
    void setOnline() { this.online = Boolean.TRUE; }
    void setOffline() { this.online = Boolean.FALSE; }
    boolean isOnline() { return online; }

    // Metodo di controllo psw al login
    boolean checkPassword(String pwd){
        return this.password.equals(DigestUtils.md5Hex(pwd));
    }

    // Ritorna lista di notifiche da recapitare e cancella quest'ultime
    List<String> getNotificationList(){
        List<String> lst = new ArrayList<>(notifiche);
        notifiche.clear();
        return lst;
    }

    public String getUsername() {
        return username;
    }

    // Aggiunge stringa da notificare ad utente
    void addNotifica(String notifica) {
        this.notifiche.add(notifica);
    }

    // Init valori transient non conservati in HM
    void initTransient(){
        online = Boolean.FALSE;
    }

    @Override
    public String toString() {
        StringBuilder strbuild = new StringBuilder();
        strbuild.append(username).append(nome).append(cognome).append(password);
        for (String str : notifiche)
            strbuild.append(str);
        strbuild.append(online);
        return strbuild.toString();
    }
}
