import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by chrx on 2/24/17.
 */
public class Client {

    public static void main(String[] args) throws IOException {

        if (args.length < 4) {
            System.out.println("Usage: java Client <hostname> <port> <oper> <opnd>*");
            return;
        }

        // get a datagram socket
        DatagramSocket socket = new DatagramSocket();

        // send request
        String cmd = "";
        for(int i = 2; i < args.length; i++)
            cmd += args[i] + " ";
        byte[] buf = cmd.getBytes();
        InetAddress address = InetAddress.getByName(args[0]);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, Integer.parseInt(args[1]));
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
