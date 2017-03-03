import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Created by chrx on 2/24/17.
 */
public class ServerThread extends Thread {

    protected DatagramSocket socket = null;
    private HashMap<String,String> licensePlates;

    public ServerThread(int port) throws SocketException {
        this("Server", port);
    }

    public ServerThread(String name, int port) throws SocketException {
        super(name);

        socket = new DatagramSocket(port);

        licensePlates = new HashMap<>();
    }


    public void run() {

        while (true) {
            try {
                byte[] buf = new byte[256];

                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                // figure out response
                String cmd = new String(packet.getData(), 0, packet.getLength());
                System.out.println(cmd);
                String response = processCommand(cmd);
                buf = response.getBytes();

                // send the response to the client at "address" and "port"
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        socket.close();
    }

    private String processCommand(String command){
        String[] params = command.split(" ");

        if(params[0].equals("register")){
            if(params.length != 3)
                return "Wrong Number of parameters for register";
            if(!Pattern.matches("\\w\\w-\\w\\w-\\w\\w", params[1]))
                return "-1";
            if(licensePlates.containsKey(params[1]))
                return "-1";

            licensePlates.put(params[1],params[2]);
            return Integer.toString(licensePlates.size());
        }
        else if(params[0].equals("lookup")){
            if(params.length != 2)
                return "Wrong Number of parameters for lookup";
            if(!licensePlates.containsKey(params[1]))
                return "NOT_FOUND";
            return licensePlates.get(params[1]);
        }
        return "UNKNOWN_COMMAND";
    }
}
