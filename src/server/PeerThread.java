package server;

import channels.*;
import file_manager.FileManager;
import file_manager.Hash;

import java.io.File;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by chrx on 4/3/17.
 */
public class PeerThread extends Thread implements Protocol {

    private static int protocolVersion;
    public static String serverID;
    private static String serviceAccessPoint;

    private static String state;

    // < fileId -> < chunkNo -> serversContainingChunk > >
    public static ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<String>>> serversContaining;
    // < fileId -> desiredReplication >
    public static ConcurrentHashMap<String,  Integer> desiredFileReplication;
    // < fileId -> savedChunks >
    public static ConcurrentHashMap<String, Set<Integer>> savedChunks;

    public static ChannelThread controlThread, backupThread, restoreThread;

//    private MulticastSocket controlChannel, backupChannel, restoreChannel;

    public PeerThread(int protocolVersion, String serverID, String serviceAccessPoint, String mcAddress, int mcPort, String mdbAddress, int mdbPort, String mdrAddress, int mdrPort) {

        PeerThread.protocolVersion = protocolVersion;
        PeerThread.serverID = serverID;
        PeerThread.serviceAccessPoint = serviceAccessPoint;

        PeerThread.serversContaining = new ConcurrentHashMap<>();
        PeerThread.desiredFileReplication = new ConcurrentHashMap<>();
        PeerThread.savedChunks = new ConcurrentHashMap<>();

        PeerThread.state = "STARTED";

        controlThread = new ControlChannelThread(mcAddress, mcPort);
        backupThread = new BackupChannelThread(mdbAddress, mdbPort);
        restoreThread = new RestoreChannelThread(mdrAddress, mdrPort);

        controlThread.start();
        backupThread.start();
        restoreThread.start();

    }


    @Override
    public void backup(String version, String senderId, String path, int replicationDegree) throws RemoteException {
        FileManager.backupFile(path, replicationDegree);
    }

    @Override
    public void restore(String version, String senderId, String path) throws RemoteException {

    }

    @Override
    public void delete(String version, String senderId, String path) throws RemoteException {

        String fileId = Hash.getFileId(new File(path));
        Message delete = new Message("DELETE", "1.0", serverID, fileId);
        delete.sendMessage(PeerThread.controlThread.getChannelSocket(), PeerThread.controlThread.getAddress(), PeerThread.controlThread.getPort());

    }

    @Override
    public void reclaim(String version, String senderId, String path) throws RemoteException {

    }

    @Override
    public String state() throws RemoteException {
        return null;
    }
}
