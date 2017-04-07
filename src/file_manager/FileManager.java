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
import java.util.concurrent.Executors;

/**
 * Created by ines on 29-03-2017.
 */
public class FileManager {

    final private static String chunksDirectory = "chunks/";
    final private static String restoredFilesDirectory = "restored_files/";

    final private static int MAX_CHUNK_SIZE = 64 * 1000;

    //http://stackoverflow.com/questions/10864317/how-to-break-a-file-into-pieces-using-java

    public static void splitFile(File f) throws IOException {

    }

    public static void mergeFiles(ArrayList<File> files, File into)
            throws IOException {
        into.getParentFile().mkdirs();
        try (BufferedOutputStream mergingStream = new BufferedOutputStream(
                new FileOutputStream(into))) {
            for (File f : files) {
                Files.copy(f.toPath(), mergingStream);
            }
        }
    }

    public static ArrayList<File> getChunks(String filename, File folder){
        ArrayList<File> chunkList = new ArrayList<>();
        for(final File fileEntry : folder.listFiles()){
            if(fileEntry.getName().contains(filename + '.'))
                chunkList.add(fileEntry);
        }
        Collections.sort(chunkList);
        return chunkList;
    }

    public static String getFileId(File file){
        return "";
    }

    public static void backupFile(String filepath, int replicationDegree){
        int chunkNo = 1;//I like to name parts from 001, 002, 003, ...
        //you can change it to 0 if you want 000, 001, ...

        File file = new File(filepath);

        byte[] buffer = new byte[MAX_CHUNK_SIZE];

        try (BufferedInputStream bis = new BufferedInputStream(
                new FileInputStream(file))) {//try-with-resources to ensure closing stream
            String name = file.getName();

            int tmp = 0;
            do {
                tmp = bis.read(buffer);
                //write each chunk of data into separate file with different number in name
                backupChunk(buffer, tmp == -1 ? 0 : tmp, replicationDegree, Hash.getFileId(file), chunkNo);
            } while (tmp == MAX_CHUNK_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void backupChunk(byte[] chunk, int chunkSize, int replicationDegree, String fileId, int chunkNo){
        Executors.newSingleThreadExecutor().execute(
                () -> {
                    int noSentCommands = 0;

                    Message putChunk = new Message("PUTCHUNK", "1.0", PeerThread.serverID, fileId, chunkNo, replicationDegree, chunk, chunkSize);
                    while(noSentCommands < 5 && PeerThread.currentChunkReplication.get(fileId).get(chunkNo) < replicationDegree) {
                        putChunk.sendMessage(PeerThread.backupThread.getChannelSocket(), PeerThread.backupThread.getAddress(), PeerThread.backupThread.getPort());
                        try {
                            Thread.sleep(2 ^ noSentCommands * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        noSentCommands++;
                    }
                    if(noSentCommands == 5)
                        System.out.println("Couldn't backup chunk appropriately");
                }
        );
    };

    public static void storeChunk(byte[] chunk, String fileId, String chunkNo){
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

    public static void deleteFile(String filepath) throws IOException {

        File file = new File(filepath);
        String fileId = Hash.getFileId(file);

        if(!PeerThread.savedChunks.containsKey(fileId)) return;

        Set<Integer> chunks = PeerThread.savedChunks.get(fileId);

        for(Integer c: chunks){
            Path path = Paths.get(chunksDirectory + fileId + c.toString());
            Files.delete(path);
        }

    }

    public static void main(String[] args) throws IOException {
//        Scanner reader = new Scanner(System.in);  // Reading from System.in
//        System.out.println("Enter the full path of the file you want to split");
//        String path = reader.nextLine();
//        backupFile(path,1);

        FileManager.deleteFile("/home/ines/SDIS/sid.jpg");
//        File toSplit = new File(path);
//        splitFile(toSplit);
//        File chunksContainer = new File(chunksDirectory);
//        ArrayList<File> chunks = getChunks(toSplit.getName(), chunksContainer);
//        for (File f:
//             chunks) {
//            System.out.println(f.getName());
//        }
//        mergeFiles(chunks, new File(restoredFilesDirectory + toSplit.getName()));
    }
}
