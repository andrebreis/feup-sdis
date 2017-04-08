package channels;

import server.Peer;
import server.PeerThread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ines on 29-03-2017.
 */
public class ControlChannelThread extends ChannelThread {


    public ControlChannelThread(String address, int port) {
        super(address, port);
    }

    public void processStored(String[] headerParams) {

        String fileId = headerParams[FILE_ID];
        int chunkNo = Integer.parseInt(headerParams[CHUNK_NO]);


        if(!PeerThread.serversContaining.containsKey(fileId))
            PeerThread.serversContaining.put(fileId, new ConcurrentHashMap<>());

        if(!PeerThread.serversContaining.get(fileId).containsKey(chunkNo))
            PeerThread.serversContaining.get(fileId).put(chunkNo, new HashSet<>());

        PeerThread.serversContaining.get(fileId).get(chunkNo).add(headerParams[SENDER_ID]);
    }

    public void processMessage(byte[] message, int length) {

        int headerLength = Message.getHeaderLength(message, length);
        if (headerLength == -1) {
            System.out.println("Message Header doesn't end with <CRLF><CRLF>");
            return;
        }

        String headerString = new String(message, 0, headerLength);

        String[] messageParams = headerString.split("\\s+");

        if (messageParams[0].equals("STORED")) {
            processStored(messageParams);
        }

    }


    public void run() {
        while (true) {

            byte[] buffer = new byte[MAX_HEADER_SIZE + MAX_CHUNK_SIZE];

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
