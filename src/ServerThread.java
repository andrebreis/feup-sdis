import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

/**
 * Created by chrx on 2/24/17.
 */
public class ServerThread extends Thread {

    private class BroadcastAddress extends TimerTask {

        private String msg;
        private InetAddress group;
        private MulticastSocket socket;
        private int port;

        public BroadcastAddress(MulticastSocket socket , InetAddress group, String msg, int port){
            this.socket = socket;
            this.group = group;
            this.msg = msg;
            this.port = port;
        }

        public void run(){

            DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(),
                    group, port);
//            System.out.println(msg);
            try {
                socket.setTimeToLive(1);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    protected DatagramSocket socket = null;
    private HashMap<String,String> licensePlates;
//    protected BroadcastAddress multicastThread = null;


    public ServerThread(int serverPort, String multicastAddress, int multicastPort) throws IOException {
        this("Server", serverPort, multicastAddress, multicastPort);
    }

    public ServerThread(String name, int serverPort, String multicastAddress, int multicastPort) throws IOException {
        super(name);

        socket = new DatagramSocket(serverPort);

        licensePlates = new HashMap<>();

        // MULTICAST
        System.out.println(multicastAddress);
        InetAddress multicastAddr = InetAddress.getByName(multicastAddress);
        MulticastSocket multicastSocket = new MulticastSocket(multicastPort);
        multicastSocket.joinGroup(multicastAddr);

        InetAddress hostAddr = InetAddress.getLocalHost();
        String msg = hostAddr.getHostAddress() + " " + serverPort;

        Timer timer = new Timer();
        timer.schedule(new BroadcastAddress(multicastSocket, multicastAddr, msg, multicastPort), 0, 1000);

    }


    public void run() {

        while (true) {
            try {
                byte[] buf = new byte[512];

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
