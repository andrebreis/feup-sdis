package channels;

import file_manager.FileManager;
import server.PeerThread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by chrx on 4/6/17.
 */
public class RestoreChannelThread extends ChannelThread {


    public RestoreChannelThread(String address, int port) {
        super(address, port);
    }

    public void processChunk(String[] headerParams, byte[] body) {

        int serverId = Integer.parseInt(headerParams[SENDER_ID]);

        if(serverId == PeerThread.serverID) return;

        String fileId = headerParams[FILE_ID];
        int chunkNo = Integer.parseInt(headerParams[CHUNK_NO]);

        System.out.println("Got CHUNK nr " + chunkNo + " for file " + fileId);

        if(!PeerThread.sentChunks.containsKey(fileId))
            PeerThread.sentChunks.put(fileId, new HashSet<>());
        PeerThread.sentChunks.get(fileId).add(chunkNo);

        PeerThread.restoringChunks.get(fileId).put(chunkNo, body);
    }

    public void processMessage(byte[] message, int length) {

        int headerLength = Message.getHeaderLength(message, length);
        if (headerLength == -1) {
            System.out.println("Message Header doesn't end with <CRLF><CRLF>");
            return;
        }

        String headerString = new String(message, 0, headerLength);

        String[] messageParams = headerString.split("\\s+");

        byte[] body = new byte[length - headerLength];
        if (messageParams[0].equals("CHUNK")) {
            System.arraycopy(message, headerLength, body, 0, length - headerLength);
            processChunk(messageParams, body);
        }

    }

}
