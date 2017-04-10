package file_manager;

import protocols.BackupProtocol;
import server.PeerThread;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
/**
 * Created by ines on 29-03-2017.
 */
public class FileManager {

    final private static String chunksDirectory = "chunks/";
    final public static String metadataPath = ".metadata/metadata";
    final private static String restoredFilesDirectory = "restored_files/";

    public final static int MAX_CHUNK_SIZE = 64 * 1000;

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
                BackupProtocol.backupChunk(buffer, tmp == -1 ? 0 : tmp, replicationDegree, Utils.getFileId(file), chunkNo);
                chunkNo++;
            } while (tmp == MAX_CHUNK_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

        for(Integer c: chunks)
            deleteChunk(fileId, c);

        PeerThread.savedChunks.remove(fileId);
        PeerThread.saveMetadata();
    }

    public static int deleteChunk(String fileId, int chunkNo) {
        int size = (int) new File(chunksDirectory + fileId + chunkNo).length();
        Path path = Paths.get(chunksDirectory + fileId + chunkNo);
        try {
            Files.delete(path);
            return size;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
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
}
