import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Created by chrx on 2/24/17.
 */
public class Client {

    public static void main(String[] args) throws IOException {

        if (args.length < 4) {
            System.out.println("java Client <mcast_addr> <mcast_port> <oper> <opnd> * ");
            return;
        }

        // MULTICAST GROUP JOIN
        byte[] receiver = new byte[64];
        InetAddress multicastAddr = InetAddress.getByName(args[0]);
        MulticastSocket multicastSocket = new MulticastSocket(Integer.parseInt(args[1]));
        multicastSocket.joinGroup(multicastAddr);

        // MULTICAST GROUP LISTEN
        DatagramPacket hostLocation = new DatagramPacket(receiver, receiver.length, multicastAddr, Integer.parseInt(args[1]));
        multicastSocket.receive(hostLocation);

        // PARSE MESSAGE
        String host = new String(hostLocation.getData(), 0, hostLocation.getLength());
        String[] parts = host.split(" ");
        String address = parts[0];
        String port = parts[1];


        // get a datagram socket
        DatagramSocket socket = new DatagramSocket();

        // send request
        String cmd = "";
        for(int i = 2; i < args.length; i++)
            cmd += args[i] + " ";
        byte[] buf = cmd.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(address), Integer.parseInt(port));
        socket.send(packet);

        // get response
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);

        // display response
        String received = new String(packet.getData(), 0, packet.getLength());
        System.out.println(received);

        socket.close();
    }

}
