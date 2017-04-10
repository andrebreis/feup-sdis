package server;

import channels.*;
import file_manager.FileManager;
import file_manager.Hash;

import java.io.*;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by chrx on 4/3/17.
 */
public class PeerThread extends Thread implements Protocol {

    private static String protocolVersion;
    public static String serverID;
    private static int serviceAccessPoint;

    private static String state;

    // < fileId -> < chunkNo -> serversContainingChunk > >
    public static ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<String>>> serversContaining;
    // < fileId -> desiredReplication >
    public static ConcurrentHashMap<String,  Integer> desiredFileReplication;
    // < fileId -> savedChunks >
    public static ConcurrentHashMap<String, Set<Integer>> savedChunks;

    public static ConcurrentHashMap<String, Set<Integer>> sentChunks;

    public static ConcurrentHashMap<String, ConcurrentHashMap<Integer, byte[]>> restoringChunks;

    public static ConcurrentHashMap<String, String> fileIds;

    public static ChannelThread controlThread, backupThread, restoreThread;

    public PeerThread(String protocolVersion, String serverID, int serviceAccessPoint, String mcAddress, int mcPort, String mdbAddress, int mdbPort, String mdrAddress, int mdrPort) {

        PeerThread.protocolVersion = protocolVersion;
        PeerThread.serverID = serverID;
        PeerThread.serviceAccessPoint = serviceAccessPoint;


        PeerThread.serversContaining = new ConcurrentHashMap<>();
        PeerThread.desiredFileReplication = new ConcurrentHashMap<>();
        PeerThread.savedChunks = new ConcurrentHashMap<>();
        PeerThread.restoringChunks = new ConcurrentHashMap<>();
        PeerThread.fileIds = new ConcurrentHashMap<>();
        PeerThread.sentChunks = new ConcurrentHashMap<>();
        loadMetadata();

        PeerThread.state = "STARTED";

        controlThread = new ControlChannelThread(mcAddress, mcPort);
        backupThread = new BackupChannelThread(mdbAddress, mdbPort);
        restoreThread = new RestoreChannelThread(mdrAddress, mdrPort);

        controlThread.start();
        backupThread.start();
        restoreThread.start();

    }

    @SuppressWarnings("unchecked")
    public void loadMetadata() {
        File metadata = new File(FileManager.metadataPath);

        if(!metadata.exists()) {
            metadata.getParentFile().mkdirs();
            try {
                metadata.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                FileInputStream fileIn = new FileInputStream(metadata);
                ObjectInputStream in = new ObjectInputStream(fileIn);

                PeerThread.serversContaining = (ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<String>>>) in.readObject();
                PeerThread.desiredFileReplication = (ConcurrentHashMap<String, Integer>) in.readObject();
                PeerThread.savedChunks = (ConcurrentHashMap<String, Set<Integer>>) in.readObject();

                in.close();
                fileIn.close();
            } catch (IOException | ClassNotFoundException ignored) {
            }
        }
    }

    public static void saveMetadata() {
        File metadata = new File(FileManager.metadataPath);
        if(!metadata.exists()) {
            metadata.getParentFile().mkdirs();
            try {
                metadata.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                FileOutputStream fileOut = new FileOutputStream(metadata);
                ObjectOutputStream out = new ObjectOutputStream(fileOut);

                out.writeObject(serversContaining);
                out.writeObject(desiredFileReplication);
                out.writeObject(savedChunks);

                out.close();
                fileOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    @Override
    public void backup(String version, String senderId, String path, int replicationDegree) throws RemoteException {
        fileIds.put(path,Hash.getFileId(new File(path)));
        FileManager.backupFile(path, replicationDegree);
    }

    @Override
    public void restore(String version, String senderId, String path) throws RemoteException {
        String fileId = fileIds.get(path);
        restoringChunks.put(fileId,new ConcurrentHashMap<>());
        FileManager.restoreFile(path);
    }

    @Override
    public void delete(String version, String senderId, String path) throws RemoteException {

        String fileId = Hash.getFileId(new File(path));
        Message delete = new Message("DELETE", "1.0", serverID, fileId);
        delete.sendMessage(PeerThread.controlThread.getChannelSocket(), PeerThread.controlThread.getAddress(), PeerThread.controlThread.getPort());

    }

    @Override
    public void reclaim(String version, String senderId, int space) throws RemoteException {

    }

    @Override
    public String state() throws RemoteException {
        return null;
    }

    public static int getCurrentReplication(String fileId, int chunkNo){
        try {
            return serversContaining.get(fileId).get(chunkNo).size();
        } catch (NullPointerException npe) {
            return 0;
        }
    }
}
