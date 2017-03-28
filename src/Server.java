import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.regex.Pattern;

public class Server implements RemoteInterface {
    private HashMap<String,String> licensePlates;


    public Server() {
        licensePlates = new HashMap<>();
    }

    public static void main(String args[]) {

        if (args.length < 1) {
            System.out.println("java Server <remote_object_name>");
            return;
        }

        try {
            Server obj = new Server();
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(obj, 1099);

            LocateRegistry.createRegistry(1099);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(args[0], stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public String register(String plate, String owner) throws RemoteException {

        if(!Pattern.matches("\\w\\w-\\w\\w-\\w\\w", plate))
            return "-1";
        if(licensePlates.containsKey(plate))
            return "-1";

        licensePlates.put(plate,owner);
        return Integer.toString(licensePlates.size());

    }

    @Override
    public String lookup(String plate) throws RemoteException {

        if(!licensePlates.containsKey(plate))
            return "NOT_FOUND";
        return licensePlates.get(plate);
    }
}
