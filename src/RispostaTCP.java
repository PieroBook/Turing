import java.util.List;

class RispostaTCP {
    private int esito;
    private List<Documento> files;
    private List<String> notifiche;

    RispostaTCP(int e, List<Documento> f, List<String> n){
        this.esito = e;
        this.files = f;
        this.notifiche = n;
    }

    RispostaTCP(int e){
        this.esito = e;
        this.files = null;
        this.notifiche = null;
    }

    int getEsito() {
        return esito;
    }

    List<Documento> getFiles() {
        return files;
    }

    List<String> getNotifiche() {
        return notifiche;
    }

}
