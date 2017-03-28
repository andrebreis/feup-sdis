import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    public static void main(String[] args) throws IOException {

        if (args.length < 4) {
            System.out.println("java Client <host_name> <remote_object_name> <oper> <opnd> * ");
            return;
        }

        try {
            Registry registry = LocateRegistry.getRegistry(args[0]);
            RemoteInterface stub = (RemoteInterface) registry.lookup(args[1]);

            String response = "";

            if(args[2].equals("register")){
                if(args.length < 5) return;
                response = stub.register(args[3],args[4]);
            }
            else if (args[2].equals("lookup")){
                if(args.length < 4) return;
                response = stub.lookup(args[3]);
            }
            System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }

    }

}
