import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {
    String register(String owner, String plate) throws RemoteException;
    String lookup(String plate) throws RemoteException;
}

