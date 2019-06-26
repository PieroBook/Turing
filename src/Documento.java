import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

class Documento {
    private String nomefile;
    private String owner;
    private int numsezioni;
    private List<String> sharedWith;
    transient private List<String> editBy;
    transient private List<List<String>> wantToEdit;

    Documento(String owner,String name,int num){
        this.nomefile = name;
        this.numsezioni = num;
        this.owner = owner;
        this.sharedWith = Collections.synchronizedList(new ArrayList<>());
        // Aggiunge owner a lista condivisori del documento
        this.sharedWith.add(owner);
        initTransient();
    }

    String getOwner(){
        return this.owner;
    }
    int getNumsezioni() {
        return numsezioni;
    }
    String getNomefile() {
        return nomefile;
    }

    // Metodo aggiunta condivisori del file (Proprietario incluso)
    boolean addShare(String s){
        // Controlla se gia condiviso con l'utente s
        if( sharedWith.indexOf(s) != -1)
            return false;
        // Aggiunge condivisore
        sharedWith.add(s);
        return true;
    }

    // Ritorna la lista di utenti con cui il doc e' condiviso
    List<String> getSharedWith() {
        return sharedWith;
    }

    // Ritorna username utente che sta editando la sezione
    String getEditingUser(int sez){
        return editBy.get(--sez);
    }

    // Aggiunge utente alla lista di edit o di wait
    // Ritorna null o l'username di chi sta editando
    String addEdit(String user, int sezione){
        // Riallineamento indice
        sezione--;
        String usr = editBy.get(sezione);
        if(usr == null){
            editBy.set(sezione,user);
        }else{
            for(String u :wantToEdit.get(sezione))
                if(u.equals(user))
                    return usr;
            wantToEdit.get(sezione).add(user);
        }
        return usr;
    }

    // Rimuove l'username dell'utente che ha finito di editare la sezione
    List<String> removeEdit(int sezione){
        sezione--;
        editBy.set(sezione,null);
        List<String> attesaFineEdit = wantToEdit.get(sezione);
        if(!attesaFineEdit.isEmpty())
            return attesaFineEdit;
        return null;
    }

    // Cancella la lista di utenti in attesa di editing
    void clearAttesa(int sezione){
        wantToEdit.get(--sezione).clear();
    }

    // Inizializzazione di strutture dati che non vengono conservate dal server
    void initTransient(){
        editBy = Collections.synchronizedList(new ArrayList<>(this.numsezioni));
        wantToEdit = Collections.synchronizedList(new ArrayList<>(this.numsezioni));
        for(int i=0; i< this.numsezioni;i++){
            editBy.add(i,null);
            wantToEdit.add(i,new ArrayList<>());
        }
    }

    // Ritorna una stringa di notifica che informa su chi sta editando e cosa e' in edit
    String getEditingNotificationString(int sez){
        if(sez != 0){
            sez--;
            if(editBy.get(sez) == null)
                return null;
            return "La sezione "+(sez+1)+" è in edit da parte dell'utente "+editBy.get(sez);
        }
        StringBuilder strb = new StringBuilder();
        boolean some = false;
        for(int i=0;i<numsezioni;i++){
            if(editBy.get(i)!=null){
                some = true;
                strb.append("La sezione ").append(i+1).append(" è in edit da parte dell'utente ")
                        .append(editBy.get(i)).append("\n");
            }
        }
        return some?strb.toString():null;
    }
}
