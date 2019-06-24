import java.rmi.Remote;
import java.rmi.RemoteException;

public interface InterfaceUser extends Remote {
    // Metodo esposto tramite RMI
    boolean registraUtente(String name,String surname,String username,String password) throws RemoteException;
}
