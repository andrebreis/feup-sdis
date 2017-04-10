package file_manager;

import channels.Message;
import server.PeerThread;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static server.PeerThread.restoringChunks;

/**
 * Created by ines on 29-03-2017.
 */
public class FileManager {

    final private static String chunksDirectory = "chunks/";
    final public static String metadataPath = ".metadata/metadata";
    final private static String restoredFilesDirectory = "restored_files/";

    final private static int MAX_CHUNK_SIZE = 64 * 1000;
    final private static int MAX_CHUNKS_PER_REQUEST = 10;

    static ExecutorService threadPool;

    //http://stackoverflow.com/questions/10864317/how-to-break-a-file-into-pieces-using-java

    public static void mergeFile(String filepath){

        File restoredFile = new File(restoredFilesDirectory + new File(filepath).getName());
        try {
            restoredFile.getParentFile().mkdirs();
            restoredFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String fileId = PeerThread.fileIds.get(filepath);

        try (BufferedOutputStream mergingStream = new BufferedOutputStream(
                new FileOutputStream(restoredFile))) {
            for (byte[] chunk : PeerThread.restoringChunks.get(fileId).values()) {
                mergingStream.write(chunk);
            }
            mergingStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<File> getChunks(File folder) {
        ArrayList<File> chunkList = new ArrayList<>();
        for (final File fileEntry : folder.listFiles()) {
            chunkList.add(fileEntry);
        }
        Collections.sort(chunkList);
        return chunkList;
    }

    public static void backupFile(String filepath, int replicationDegree) {
        int chunkNo = 1;//I like to name parts from 001, 002, 003, ...
        //you can change it to 0 if you want 000, 001, ...

        File file = new File(filepath);

        try (BufferedInputStream bis = new BufferedInputStream(
                new FileInputStream(file))) {//try-with-resources to ensure closing stream
            String name = file.getName();

            int tmp = 0;
            do {
                byte[] buffer = new byte[MAX_CHUNK_SIZE];
                tmp = bis.read(buffer);
                //write each chunk of data into separate file with different number in name
                backupChunk(buffer, tmp == -1 ? 0 : tmp, replicationDegree, Hash.getFileId(file), chunkNo);
                chunkNo++;
            } while (tmp == MAX_CHUNK_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void backupChunk(byte[] chunk, int chunkSize, int replicationDegree, String fileId, int chunkNo) {
        Executors.newSingleThreadExecutor().execute(
                () -> {
                    int noSentCommands = 0;

                    Message putChunk = new Message("PUTCHUNK", "1.0", PeerThread.serverID, fileId, chunkNo, replicationDegree, chunk, chunkSize);
                    while (noSentCommands < 5 && PeerThread.getCurrentReplication(fileId, chunkNo) < replicationDegree) {
                        putChunk.sendMessage(PeerThread.backupThread.getChannelSocket(), PeerThread.backupThread.getAddress(), PeerThread.backupThread.getPort());
                        try {
                            Thread.sleep(2 ^ noSentCommands * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Sending PUTCHUNK no " + Integer.toString(noSentCommands) + "for chunk no " + Integer.toString(chunkNo));
                        noSentCommands++;
                    }
                    if (noSentCommands == 5)
                        System.out.println("Couldn't backup chunk appropriately");
                    else
                        System.out.println("Backup finished successfully");
                }
        );
    }

    ;

    public static void storeChunk(byte[] chunk, String fileId, String chunkNo) {
        File chunkFile = new File(chunksDirectory, fileId + chunkNo);
        chunkFile.getParentFile().mkdirs();
        try {
            FileOutputStream out = new FileOutputStream(chunkFile);
            out.write(chunk);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteFile(String fileId) throws IOException {
        if (!PeerThread.savedChunks.containsKey(fileId)) {
            PeerThread.serversContaining.remove(fileId);
            PeerThread.desiredFileReplication.remove(fileId);
            return;
        }

        Set<Integer> chunks = PeerThread.savedChunks.get(fileId);

        for (Integer c : chunks) {
            Path path = Paths.get(chunksDirectory + fileId + c.toString());
            Files.delete(path);
        }

        PeerThread.savedChunks.remove(fileId);

        PeerThread.saveMetadata();

    }

    public static byte[] getChunk(String fileId, int chunkNo) {

        Path path = Paths.get(chunksDirectory + fileId + chunkNo);

        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void restoreChunk(String fileId, int chunkNo) {
        FileManager.threadPool.submit(
                new Thread(() -> {
                    Message getChunk = new Message("GETCHUNK", "1.0", PeerThread.serverID, fileId, Integer.toString(chunkNo));
                    do {
                        getChunk.sendMessage(PeerThread.controlThread.getChannelSocket(), PeerThread.controlThread.getAddress(), PeerThread.controlThread.getPort());
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } while (!(restoringChunks.get(fileId).containsKey(chunkNo) || receivedAllChunks(fileId)));
                }
                ));
    }

    public static void restoreFile(String filepath) {

        String fileId = Hash.getFileId(new File(filepath));

        threadPool = Executors.newFixedThreadPool(MAX_CHUNKS_PER_REQUEST);
        int chunkNo = 1;
        restoringChunks.put(fileId, new ConcurrentHashMap<>());

        do {
            for (int i = chunkNo; i < chunkNo + MAX_CHUNKS_PER_REQUEST; i++) {
                restoreChunk(fileId, i);
            }
            chunkNo += MAX_CHUNKS_PER_REQUEST;
            threadPool.shutdown();
            try {
                if(!threadPool.awaitTermination(4, TimeUnit.SECONDS)) {
                    do {
                        threadPool.shutdownNow();
                    }while (!threadPool.isTerminated());
                    return;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        } while (!receivedAllChunks(fileId));

        mergeFile(filepath);
    }


    public static boolean receivedAllChunks(String fileId) {
        byte[] lastChunk = restoringChunks.get(fileId).get(restoringChunks.get(fileId).size());
        return lastChunk != null && lastChunk.length < MAX_CHUNK_SIZE;
    }
}
