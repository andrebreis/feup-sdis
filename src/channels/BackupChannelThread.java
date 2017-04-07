package channels;

import file_manager.FileManager;
import server.PeerThread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ines on 29-03-2017.
 */
public class BackupChannelThread extends ChannelThread {


    public BackupChannelThread(String address, int port) {
        super(address, port);
    }


    public void storeChunk(String fileId, int chunkNo, int replicationDegree, byte[] chunk) {
        if (!PeerThread.savedChunks.containsKey(fileId))
            PeerThread.savedChunks.put(fileId, new HashSet<>());
        PeerThread.savedChunks.get(fileId).add(chunkNo);

        if (!PeerThread.desiredFileReplication.containsKey(fileId))
            PeerThread.desiredFileReplication.put(fileId, replicationDegree);

        if (!PeerThread.serversContaining.containsKey(fileId))
            PeerThread.serversContaining.put(fileId, new ConcurrentHashMap<>());

        if(!PeerThread.serversContaining.get(fileId).containsKey(chunkNo))
            PeerThread.serversContaining.get(fileId).put(chunkNo, new HashSet<>());

        PeerThread.serversContaining.get(fileId).get(chunkNo).add(PeerThread.serverID);

        FileManager.storeChunk(chunk, fileId, Integer.toString(chunkNo));
    }

    //TODO: enhancement wait first and store after
    public void processPutChunk(String[] headerParams, byte[] body) {

        int chunkNumber = Integer.parseInt(headerParams[CHUNK_NO]);
        String fileID = headerParams[FILE_ID];

        if (headerParams[SENDER_ID].equals(PeerThread.serverID))
            return;

        //store chunk
        if (!PeerThread.savedChunks.containsKey(fileID) || !PeerThread.savedChunks.get(fileID).contains(chunkNumber))
            storeChunk(fileID, chunkNumber, Integer.parseInt(headerParams[REPLICATION_DEG]), body);

        Message msg = new Message("STORED", headerParams[VERSION], headerParams[SENDER_ID], headerParams[FILE_ID], headerParams[CHUNK_NO]);
        msg.sendMessageWithDelay(PeerThread.controlThread.channelSocket, PeerThread.controlThread.address, PeerThread.controlThread.port);
    }

    public void processMessage(byte[] message, int length) {

        int headerLength = Message.getHeaderLength(message, length);
        if (headerLength == -1) {
            System.out.println("Message Header doesn't end with <CRLF><CRLF>");
            return;
        }

        String headerString = new String(message, 0, headerLength);

        String[] messageParams = headerString.split(" ");

        byte[] body = new byte[length - headerLength];
        if (messageParams[0].equals("PUTCHUNK")) {
            System.arraycopy(message, headerLength, body, 0, length - headerLength);
            processPutChunk(messageParams, body);
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
