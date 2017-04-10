package file_manager;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by ines on 07-04-2017.
 */
public class Utils {

    final public static int AVOID_CONCURRENCY_SLEEP_DURATION = 401;
    final public static int NO_BACKUP_TRIES = 5;
    final public static int SECONDS_TO_MILLISECONDS = 1000;


    private static byte[] sha256(String file){

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

    public static String getFileId(File file){

        long lastModified = file.lastModified();

        String bitString = file.getName()+ Long.toString(lastModified);
        return DatatypeConverter.printHexBinary(sha256(bitString));
    }

}
