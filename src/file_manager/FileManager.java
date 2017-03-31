package file_manager;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

/**
 * Created by ines on 29-03-2017.
 */
public class FileManager {

    final private static int MAX_CHUNK_SIZE = 64 * 1000;

    //http://stackoverflow.com/questions/10864317/how-to-break-a-file-into-pieces-using-java

    public static void splitFile(File f) throws IOException {
        int partCounter = 1;//I like to name parts from 001, 002, 003, ...
        //you can change it to 0 if you want 000, 001, ...

        byte[] buffer = new byte[MAX_CHUNK_SIZE];

        try (BufferedInputStream bis = new BufferedInputStream(
                new FileInputStream(f))) {//try-with-resources to ensure closing stream
            String name = f.getName();

            int tmp = 0;
            do {
                tmp = bis.read(buffer);
                        //write each chunk of data into separate file with different number in name
                File newFile = new File(f.getParent(), name + "."
                        + String.format("%05d", partCounter++));
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    out.write(buffer, 0, tmp == -1 ? 0 : tmp);//tmp is chunk size
                }
            } while (tmp == MAX_CHUNK_SIZE);
        }
    }

    public static void mergeFiles(ArrayList<File> files, File into)
            throws IOException {
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

    public static void main(String[] args) throws IOException {
        Scanner reader = new Scanner(System.in);  // Reading from System.in
        System.out.println("Enter the full path of the file you want to split");
        String path = reader.nextLine();
        splitFile(new File(path));
        File typeOf = new File("/media/chrx/9016-4EF8/");
        ArrayList<File> chunks = getChunks("exp6_step5.pcapng", typeOf);
        for (File f:
             chunks) {
            System.out.println(f.getName());
        }
        mergeFiles(chunks, new File("/media/chrx/9016-4EF8/new.pcapng"));
    }
}
