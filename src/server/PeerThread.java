package server;

import channels.BackupChannelThread;
import channels.ControlChannelThread;
import channels.Message;
import channels.RestoreChannelThread;
import file_manager.FileManager;
import file_manager.Utils;
import protocols.Protocol;
import protocols.RestoreProtocol;

import java.io.*;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by chrx on 4/3/17.
 */
public class PeerThread extends Thread implements Protocol {

    public static int serverID;
    // < fileId -> < chunkNo -> serversContainingChunk > >
    public static ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<Integer>>> serversContaining;
    // < fileId -> desiredReplication >
    public static ConcurrentHashMap<String, Integer> desiredFileReplication;
    // < fileId -> savedChunks >
    public static ConcurrentHashMap<String, Set<Integer>> savedChunks;
    public static ConcurrentHashMap<String, Set<Integer>> sentChunks;
    public static ConcurrentHashMap<String, ConcurrentHashMap<Integer, byte[]>> restoringChunks;
    public static ConcurrentHashMap<String, String> fileIds;
    public static ConcurrentHashMap<String, Set<Integer>> chunksToBackup;
    public static ControlChannelThread controlThread;
    public static BackupChannelThread backupThread;
    public static RestoreChannelThread restoreThread;
    private static String protocolVersion;
    private static int serviceAccessPoint;
    private static int maximumSpace = -1;
    private static int usedSpace = 0;
    private static String state;

    PeerThread(String protocolVersion, int serverID, int serviceAccessPoint, String mcAddress, int mcPort, String mdbAddress, int mdbPort, String mdrAddress, int mdrPort) {

        PeerThread.protocolVersion = protocolVersion;
        PeerThread.serverID = serverID;
        PeerThread.serviceAccessPoint = serviceAccessPoint;


        PeerThread.serversContaining = new ConcurrentHashMap<>();
        PeerThread.desiredFileReplication = new ConcurrentHashMap<>();
        PeerThread.savedChunks = new ConcurrentHashMap<>();
        PeerThread.restoringChunks = new ConcurrentHashMap<>();
        PeerThread.fileIds = new ConcurrentHashMap<>();
        PeerThread.sentChunks = new ConcurrentHashMap<>();
        PeerThread.chunksToBackup = new ConcurrentHashMap<>();
        loadMetadata();

        PeerThread.state = "STARTED";

        controlThread = new ControlChannelThread(mcAddress, mcPort);
        backupThread = new BackupChannelThread(mdbAddress, mdbPort);
        restoreThread = new RestoreChannelThread(mdrAddress, mdrPort);

        controlThread.start();
        backupThread.start();
        restoreThread.start();

    }

    public static void saveMetadata() {
        File metadata = new File(FileManager.metadataPath);
        if (!metadata.exists()) {
            metadata.getParentFile().mkdirs();
            try {
                metadata.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                FileOutputStream fileOut = new FileOutputStream(metadata);
                ObjectOutputStream out = new ObjectOutputStream(fileOut);

                out.writeObject(serversContaining);
                out.writeObject(desiredFileReplication);
                out.writeObject(savedChunks);
                out.writeObject(fileIds);
                out.writeInt(usedSpace);
                out.writeInt(maximumSpace);

                out.close();
                fileOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static int getCurrentReplication(String fileId, int chunkNo) {
        try {
            return serversContaining.get(fileId).get(chunkNo).size();
        } catch (NullPointerException npe) {
            return 0;
        }
    }

    public static void storeChunk(String fileId, int chunkNo, int replicationDegree, byte[] chunk) {
        if (!PeerThread.savedChunks.containsKey(fileId))
            PeerThread.savedChunks.put(fileId, new HashSet<>());
        PeerThread.savedChunks.get(fileId).add(chunkNo);

        if (!PeerThread.desiredFileReplication.containsKey(fileId))
            PeerThread.desiredFileReplication.put(fileId, replicationDegree);

        if (!PeerThread.serversContaining.containsKey(fileId))
            PeerThread.serversContaining.put(fileId, new ConcurrentHashMap<>());

        if (!PeerThread.serversContaining.get(fileId).containsKey(chunkNo))
            PeerThread.serversContaining.get(fileId).put(chunkNo, new HashSet<>());

        PeerThread.serversContaining.get(fileId).get(chunkNo).add(PeerThread.serverID);

        FileManager.storeChunk(chunk, fileId, Integer.toString(chunkNo));
        PeerThread.updateUsedSpace(chunk.length);
        PeerThread.saveMetadata();
    }

    public static boolean canSaveChunk(int chunkSize) {
        return (maximumSpace == -1 || usedSpace + chunkSize <= maximumSpace);
    }

    private static void freeSpace() {
        if (maximumSpace >= usedSpace) return;

        System.out.println("Too much used space");

        deleteOverReplicatedChunks();

        if (usedSpace > maximumSpace) deleteChunksOverCapacity();

        saveMetadata();
    }

    public static void deleteOverReplicatedChunks() {
        Iterator it = savedChunks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Set<Integer>> pair = (Map.Entry<String, Set<Integer>>)it.next();
            String fileId = pair.getKey();

            Iterator setIt = pair.getValue().iterator();
            while (setIt.hasNext()){
                Integer chunkNo = (Integer) setIt.next();
                if (serversContaining.get(fileId).get(chunkNo).size() > desiredFileReplication.get(fileId)) {
                    serversContaining.get(fileId).remove(chunkNo);
                    savedChunks.get(fileId).remove(chunkNo);
                    if (savedChunks.get(fileId).size() == 0) savedChunks.remove(fileId);

                    usedSpace -= FileManager.deleteChunk(fileId, chunkNo);

                    Message msg = new Message(Message.RECLAIM, "1.0", serverID, fileId, chunkNo.toString());
                    msg.sendMessage(controlThread.getChannelSocket(), controlThread.getAddress(), controlThread.getPort());
                }
            }

        }
    }

    private static void deleteChunksOverCapacity() {
        Iterator it = savedChunks.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String, Set<Integer>> pair = (Map.Entry<String, Set<Integer>>)it.next();
            String fileId = pair.getKey();

            Iterator setIt = pair.getValue().iterator();
            while (setIt.hasNext()){
                Integer chunkNo = (Integer) setIt.next();
                if (serversContaining.get(fileId).get(chunkNo).size() > 1) {
                    serversContaining.get(fileId).remove(chunkNo);
                    savedChunks.get(fileId).remove(chunkNo);
                    if (savedChunks.get(fileId).size() == 0) savedChunks.remove(fileId);

                    usedSpace -= FileManager.deleteChunk(fileId, chunkNo);

                    Message msg = new Message(Message.RECLAIM, "1.0", serverID, fileId, chunkNo.toString());
                    msg.sendMessage(controlThread.getChannelSocket(), controlThread.getAddress(), controlThread.getPort());

                    if (usedSpace <= maximumSpace) break;
                }
            }

            if (usedSpace <= maximumSpace) break;

        }

        if (usedSpace > maximumSpace)
            System.out.println("Can't free enough chunks while maintaining a copy in any other peer!");

    }

    private static void updateUsedSpace(int size) {
        usedSpace += size;
    }

    @SuppressWarnings("unchecked")
    private void loadMetadata() {
        File metadata = new File(FileManager.metadataPath);

        if (!metadata.exists()) {
            metadata.getParentFile().mkdirs();
            try {
                metadata.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                FileInputStream fileIn = new FileInputStream(metadata);
                ObjectInputStream in = new ObjectInputStream(fileIn);

                PeerThread.serversContaining = (ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<Integer>>>) in.readObject();
                PeerThread.desiredFileReplication = (ConcurrentHashMap<String, Integer>) in.readObject();
                PeerThread.savedChunks = (ConcurrentHashMap<String, Set<Integer>>) in.readObject();
                PeerThread.fileIds = (ConcurrentHashMap<String, String>) in.readObject();
                PeerThread.usedSpace = in.readInt();
                PeerThread.maximumSpace = in.readInt();

                in.close();
                fileIn.close();
            } catch (IOException | ClassNotFoundException ignored) {
            }
        }
    }

    @Override
    public void backup(String version, String senderId, String path, int replicationDegree) throws RemoteException {
        String fileId = Utils.getFileId(new File(path));
        fileIds.put(path, fileId);
        desiredFileReplication.put(fileId, replicationDegree);
        FileManager.backupFile(path, replicationDegree);
    }

    @Override
    public void restore(String version, String senderId, String path) throws RemoteException {
        String fileId = fileIds.get(path);
        if (fileId == null) {
            System.out.println("Trying to restore Unrecognized File");
            return;
        }
        restoringChunks.put(fileId, new ConcurrentHashMap<>());
        RestoreProtocol.restoreFile(path);
    }

    @Override
    public void delete(String version, String senderId, String path) throws RemoteException {
        String fileId = Utils.getFileId(new File(path));
        Message delete = new Message(Message.INIT_DELETE, "1.0", serverID, fileId);
        delete.sendMessage(controlThread.getChannelSocket(), controlThread.getAddress(), controlThread.getPort());
    }

    @Override
    public void reclaim(String version, String senderId, int space) throws RemoteException {
        maximumSpace = space;
        freeSpace();
    }

    @Override
    public String state() throws RemoteException {
        String finalString = "Initiated Backups:\n";
        for (Map.Entry<String, String> pair : fileIds.entrySet()) {
            String filepath = pair.getKey();
            String fileId = pair.getValue();

            finalString += String.format("File path: %s\n File id: %s\nDesired Replication Degree: %d\nList of: Chunk - Replication\n", filepath, fileId, desiredFileReplication.get(fileId));
            for (int chunkNo = 1; chunkNo <= serversContaining.get(fileId).size(); chunkNo++)
                finalString += String.format("%d - %d\n", chunkNo, serversContaining.get(fileId).get(chunkNo).size());

            finalString += "\n";
        }

        finalString += "Saved Chunks:\n";
        for (Map.Entry<String, Set<Integer>> pair :
                savedChunks.entrySet()) {
            String fileId = pair.getKey();
            Set<Integer> chunks = pair.getValue();

            for (Integer chunk :
                    chunks) {
                int chunkSize = (int) new File(FileManager.chunksDirectory + fileId + chunk).length();
                finalString += String.format("File: %s , Chunk Number: %d , Chunk Size: %d, Chunk Replication: %d\n", fileId, chunk, chunkSize, serversContaining.get(fileId).get(chunk).size());
            }
        }

        finalString += String.format("Maximum Capacity: %s\nUsed Capacity: %d\n", maximumSpace == -1 ? "INFINITE" : maximumSpace, usedSpace);

        return finalString;
    }

}
