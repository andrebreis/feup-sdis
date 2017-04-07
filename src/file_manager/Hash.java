package file_manager;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

/**
 * Created by ines on 07-04-2017.
 */
public class Hash {

    public byte[] sha256(String file){

        MessageDigest md = null;

        try{
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        byte[] hash = new byte[0];

        try{
            hash = md.digest(file.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return hash;
    }

    public String getFileId(File file){

        long lastModified = file.lastModified();

        String bitString = file.getName()+ Long.toString(lastModified);
        String fileId = DatatypeConverter.printHexBinary(sha256(bitString));

        return fileId;
    }

    public static void main(String[] args) throws IOException {

        System.out.println(new Hash().getFileId(new File("/home/ines/SDIS/feup-sdis/src/TestApp.java")));

    }






}
