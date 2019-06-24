class RichiestaTCP {
    private int codop;
    private String username;
    private String password;
    private String nomefile;
    private int numsezioni;
    private String condivisore;

    // Costruttore login
    RichiestaTCP(int codop, String username, String password){
        this.codop = codop;
        this.username = username;
        this.password = password;
        this.nomefile = null;
        this.condivisore = null;
    }
    // Costruttore logout/deregistrazione/lista
    RichiestaTCP(int codop, String username){
        this.codop = codop;
        this.username = username;
        this.password = null;
        this.nomefile = null;
        this.numsezioni = 0;
        this.condivisore = null;
    }
    // Costruttore CreaFile
    RichiestaTCP(int codop, String username, String nomefile, int numsezioni){
        this.codop = codop;
        this.username = username;
        this.password = null;
        this.nomefile = nomefile;
        this.numsezioni = numsezioni;
        this.condivisore = null;
    }
    // Costruttore ADD/DEL condivisore
    RichiestaTCP(int codop, String username, String nomefile, String condivisore){
        this.codop = codop;
        this.username = username;
        this.password = null;
        this.nomefile = nomefile;
        this.numsezioni = 0;
        this.condivisore = condivisore;
    }

    int getCodop() {
        return codop;
    }

    String getCondivisore() {
        return condivisore;
    }

    String getNomefile() {
        return nomefile;
    }

    int getNumsezioni() {
        return numsezioni;
    }

    String getPassword() {
        return password;
    }

    String getUsername() {
        return username;
    }
}
