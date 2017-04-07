package channels;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Created by chrx on 4/6/17.
 */
public class ChannelThread extends Thread {

    protected final int VERSION = 1, SENDER_ID = 2, FILE_ID = 3, CHUNK_NO = 4, REPLICATION_DEG = 5;

    protected static final int MAX_HEADER_SIZE = 1024;
    protected static final int MAX_CHUNK_SIZE = 64 * 1000;
    protected MulticastSocket channelSocket;
    protected InetAddress address;
    protected int port;

    public ChannelThread(String address, int port){

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

}
