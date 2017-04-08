import server.Protocol;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Created by ines on 06-04-2017.
 */
public class TestApp {

    // $ java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2(opcional-rd)>
    // $ java TestApp 1923 RESTORE test1.pdf 3

    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2(opcional-rd)>");
        }

        String peerAccessPoint = args[0];
        String protocol = args[1].toUpperCase();

        try {
            Registry registry = LocateRegistry.getRegistry();
            Protocol initiatorPeer = (Protocol) registry.lookup(peerAccessPoint);

            String path = "";
            switch (protocol) {
                case "BACKUP":
                    if (args.length != 4)
                        System.out.println("Usage for BACKUP: java TestApp <peer_ap> BACKUP <filepath> <replication_degree>");
                    path = args[2];
                    int replicationDegree = Integer.parseInt(args[3]);
                    initiatorPeer.backup("1.0", peerAccessPoint, path, replicationDegree);
                    break;
                case "RESTORE":
                    if (args.length != 3)
                        System.out.println("Usage for RESTORE: java TestApp <peer_ap> RESTORE <filepath>");
                    path = args[2];
                    initiatorPeer.restore("1.0", peerAccessPoint, path);
                    break;
                case "DELETE":
                    if (args.length != 3)
                        System.out.println("Usage for RESTORE: java TestApp <peer_ap> DELETE <filepath>");
                    path = args[2];
                    initiatorPeer.delete("1.0", peerAccessPoint, path);
                    break;
                case "RECLAIM":
                    if (args.length != 3)
                        System.out.println("Usage for RECLAIM: java TestApp <peer_ap> RECLAIM <reclaim_space>");
                    int space = Integer.parseInt(args[2]);
                    initiatorPeer.reclaim("1.0", peerAccessPoint, space);
                    break;
                case "STATE":
                    initiatorPeer.state();
                    break;
                default:
                    System.out.println("UNKNOWN COMMAND");
            }
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }

    }

}
