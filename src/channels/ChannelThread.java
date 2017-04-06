package channels;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Created by chrx on 4/6/17.
 */
public class ChannelThread extends Thread {

    protected MulticastSocket channelSocket;

    public ChannelThread(String address, int port){

        try {
            InetAddress controlAddress = InetAddress.getByName(address);
            channelSocket = new MulticastSocket(port);
            channelSocket.joinGroup(controlAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
