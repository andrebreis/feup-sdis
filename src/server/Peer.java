package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by chrx on 4/3/17.
 */
public class Peer {

    private String protocolVersion;
    private String serverID;
    private int serviceAccessPoint;

    private String mcAddress;
    private int mcPort;

    private String mdbAddress;
    private int mdbPort;

    private String mdrAddress;
    private int mdrPort;


    private void parseArguments(String[] args) {

        protocolVersion = args[0];
        serverID = args[1];
        serviceAccessPoint = Integer.parseInt(args[2]);

        mcAddress = args[3].split(":")[0];
        mcPort = Integer.parseInt(args[3].split(":")[1]);

        mdbAddress = args[4].split(":")[0];
        mdbPort = Integer.parseInt(args[4].split(":")[1]);

        mdrAddress = args[5].split(":")[0];
        mdrPort = Integer.parseInt(args[5].split(":")[1]);

    }

    public static void main(String[] args) {

        if (args.length != 6) {
            System.out.println("Usage: java Peer <ProtocolVersion> <ServerID> <ServiceAccessPoint> <MC_addr>:<MC_port> <MDB_addr>:<MDB_port> <MDR_addr>:<MDR_port>");
            return;
        }

        Peer p = new Peer();
        p.parseArguments(args);

        PeerThread peer = new PeerThread(p.protocolVersion, p.serverID, p.serviceAccessPoint, p.mcAddress, p.mcPort, p.mdbAddress, p.mdbPort, p.mdrAddress, p.mdrPort);
        peer.start();
        try {
            Protocol rmiObject = (Protocol) UnicastRemoteObject.exportObject(peer, p.serviceAccessPoint);
            LocateRegistry.createRegistry(p.serviceAccessPoint);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(p.serverID, rmiObject);
            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

}
