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

        String peerAccessPoint = args[0];
        String protocol = args[1].toUpperCase();
        String path = "";

        Protocol initiatorPeer = null;

        try {
            Registry registry = LocateRegistry.getRegistry(args[0]);
            initiatorPeer = (Protocol) registry.lookup(peerAccessPoint);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }


        switch(protocol){

            case "BACKUP":
                int replicationDegree = Integer.parseInt(args[3]);

                try{
                    initiatorPeer.backup("1",peerAccessPoint,path,replicationDegree);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                break;

            case "RESTORE":

                try{
                    initiatorPeer.restore("1", peerAccessPoint, path);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                break;

            case "DELETE":

                try{
                    initiatorPeer.delete("1", peerAccessPoint, path);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                break;

            case "RECLAIM":

                try{
                    initiatorPeer.reclaim("1", peerAccessPoint, path);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;

            case "STATE":
                try{
                    initiatorPeer.state();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
        }

    }

}
