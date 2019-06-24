import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RmiClientInstauration implements Runnable {
    @Override
    public void run() {
        // Recupero Tab per registrazione
        Registry r;
        Remote RemoteObject = null;
        Boolean retry = Boolean.TRUE;
        // Continua a tentare fin-quando non recupera l'oggetto remoto
        while(retry){
            retry = false;
            try {
                r = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
                RemoteObject = r.lookup("Server-Turing");
            } catch (RemoteException | NotBoundException ignore) {
                retry = true;
            }
            Turing.usr = (InterfaceUser) RemoteObject;
            if(retry) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ignored) {}
            }
        }
    }
}
