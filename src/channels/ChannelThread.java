package channels;

import file_manager.FileManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Created by chrx on 4/6/17.
 */
public class ChannelThread extends Thread {

    final int VERSION = 1, SENDER_ID = 2, FILE_ID = 3, CHUNK_NO = 4, REPLICATION_DEG = 5;

    private static final int MAX_HEADER_SIZE = 1024;
    MulticastSocket channelSocket;
    InetAddress address;
    int port;

    ChannelThread(String address, int port){

        this.port = port;

        try {
            this.address = InetAddress.getByName(address);
            channelSocket = new MulticastSocket(port);
            channelSocket.joinGroup(this.address);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public MulticastSocket getChannelSocket() {
        return channelSocket;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public void processMessage(byte[] message, int length) {};

    public void run() {
        while (true) {

            byte[] buffer = new byte[MAX_HEADER_SIZE + FileManager.MAX_CHUNK_SIZE];

            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                channelSocket.receive(packet);
                processMessage(packet.getData(), packet.getLength());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
