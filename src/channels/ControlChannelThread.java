package channels;

import file_manager.FileManager;
import server.Peer;
import server.PeerThread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static file_manager.FileManager.getChunk;

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

        PeerThread.saveMetadata();
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
        else if (messageParams[0].equals("DELETE")) {
            try {
                FileManager.deleteFile(messageParams[FILE_ID]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if(messageParams[0].equals("GETCHUNK")){
            processGetChunk(messageParams);
        }

    }

    private void processGetChunk(String[] headerParams){

        if(headerParams[SENDER_ID].equals(PeerThread.serverID)) return;

        String fileId = headerParams[FILE_ID];
        int chunkNo = Integer.parseInt(headerParams[CHUNK_NO]);

        if(!PeerThread.savedChunks.containsKey(fileId) ||
                !PeerThread.savedChunks.get(fileId).contains(chunkNo)) return;

        System.out.println("I got dis");
        byte[] body = getChunk(fileId,chunkNo);

        Random generator = new Random();
        try {
            Thread.sleep(generator.nextInt(401));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(!PeerThread.sentChunks.containsKey(fileId) ||
                !PeerThread.sentChunks.get(fileId).contains(chunkNo)) {
            System.out.println("Sending CHUNK...");

            Message msg = new Message("CHUNK", headerParams[VERSION], PeerThread.serverID,
                    headerParams[FILE_ID], headerParams[CHUNK_NO], body, body.length);
            msg.sendMessage(PeerThread.restoreThread.channelSocket, PeerThread.restoreThread.address, PeerThread.restoreThread.port);
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
