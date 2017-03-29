package fileManager;

import java.io.*;
import java.util.Scanner;

/**
 * Created by ines on 29-03-2017.
 */
public class FileSplit {

    //http://stackoverflow.com/questions/10864317/how-to-break-a-file-into-pieces-using-java

    public static void splitFile(File f) throws IOException {
        int partCounter = 1;//I like to name parts from 001, 002, 003, ...
        //you can change it to 0 if you want 000, 001, ...

        int chunkSize = 64000; //64KByte
        byte[] buffer = new byte[chunkSize];

        try (BufferedInputStream bis = new BufferedInputStream(
                new FileInputStream(f))) {//try-with-resources to ensure closing stream
            String name = f.getName();

            int tmp = 0;
            while ((tmp = bis.read(buffer)) > 0) {
                //write each chunk of data into separate file with different number in name
                File newFile = new File(f.getParent(), name + "."
                        + String.format("%03d", partCounter++));
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    out.write(buffer, 0, tmp);//tmp is chunk size
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Scanner reader = new Scanner(System.in);  // Reading from System.in
        System.out.println("Enter the full path of the file you want to split");
        String path = reader.nextLine();
        splitFile(new File(path));
    }
}
