package server;

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

    public void main(String[] args){

        if(args.length != 6){
            System.out.println("Usage: java Peer <ProtocolVersion> <ServerID> <ServiceAccessPoint> <MC_addr>:<MC_port> <MDB_addr>:<MDB_port> <MDR_addr>:<MDR_port>");
            return;
        }

        new PeerThread(protocolVersion, serverID, serviceAccessPoint, mcAddress, mcPort, mdbAddress, mdbPort, mdrAddress, mdrPort).start();

    }

}
