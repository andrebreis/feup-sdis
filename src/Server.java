import java.io.IOException;
import java.net.SocketException;

/**
 * Created by chrx on 2/24/17.
 */
public class Server {

    public static void main(String[] args) throws IOException {
        if(args.length != 3){
            System.out.println("Usage: java Server <srvc_port> <mcast_addr> <mcast_port>");
            return;
        }
        new ServerThread(Integer.parseInt(args[0]), args[1], Integer.parseInt(args[2])).start();
    }

}
