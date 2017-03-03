import java.net.SocketException;

/**
 * Created by chrx on 2/24/17.
 */
public class Server {

    public static void main(String[] args) throws SocketException {
        new ServerThread(Integer.parseInt(args[0])).start();
    }

}
