import java.io.IOException;
import java.net.SocketException;

/**
 * Created by chrx on 2/24/17.
 */
public class Server {

    public static void main(String[] args) throws IOException {
        if(args.length != 1){
            System.out.println("java Server <srvc_port>");
            return;
        }
        new ServerThread(Integer.parseInt(args[0])).start();
    }

}
