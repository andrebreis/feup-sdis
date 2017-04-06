package server;

import channels.BackupChannelThread;
import channels.ControlChannelThread;
import channels.RestoreChannelThread;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by chrx on 4/3/17.
 */
public class PeerThread extends Thread {

    private static int protocolVersion;
    private static String serverID;
    private static String serviceAccessPoint;

    private static String state;

    // < fileId -> < chunkNo -> currentReplicationNr > >
    private static ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>> currentChunkReplication;
    // < fileId -> desiredReplication >
    private static ConcurrentHashMap<String,  Integer> desiredFileReplication;
    // < fileId -> savedChunks >
    private static ConcurrentHashMap<String, Set<Integer>> savedChunks;

//    private MulticastSocket controlChannel, backupChannel, restoreChannel;

    public PeerThread(int protocolVersion, String serverID, String serviceAccessPoint, String mcAddress, int mcPort, String mdbAddress, int mdbPort, String mdrAddress, int mdrPort) {

        PeerThread.protocolVersion = protocolVersion;
        PeerThread.serverID = serverID;
        PeerThread.serviceAccessPoint = serviceAccessPoint;

        PeerThread.state = "STARTED";

        new ControlChannelThread(mcAddress, mcPort).start();
        new BackupChannelThread(mdbAddress, mdbPort).start();
        new RestoreChannelThread(mdrAddress, mdrPort).start();

    }

}
