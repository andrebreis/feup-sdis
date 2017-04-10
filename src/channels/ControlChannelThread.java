package channels;

import file_manager.FileManager;
import file_manager.Utils;
import protocols.BackupProtocol;
import server.PeerThread;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ines on 29-03-2017.
 */
public class ControlChannelThread extends ChannelThread {


    public ControlChannelThread(String address, int port) {
        super(address, port);
    }

    private void processStored(String[] headerParams) {
        int serverId = Integer.parseInt(headerParams[SENDER_ID]);
        String fileId = headerParams[FILE_ID];
        int chunkNo = Integer.parseInt(headerParams[CHUNK_NO]);


        if(!PeerThread.serversContaining.containsKey(fileId))
            PeerThread.serversContaining.put(fileId, new ConcurrentHashMap<>());

        if(!PeerThread.serversContaining.get(fileId).containsKey(chunkNo))
            PeerThread.serversContaining.get(fileId).put(chunkNo, new HashSet<>());

        PeerThread.serversContaining.get(fileId).get(chunkNo).add(serverId);
        System.out.println("Server " + headerParams[SENDER_ID] + " saved chunk nr" + chunkNo + " for file " + fileId);

        PeerThread.saveMetadata();
    }


    private void processGetChunk(String[] headerParams){

        int serverId = Integer.parseInt(headerParams[SENDER_ID]);
        if(serverId == PeerThread.serverID) return;

        String fileId = headerParams[FILE_ID];
        int chunkNo = Integer.parseInt(headerParams[CHUNK_NO]);

        System.out.println("Processing GETCHUNK for chunk nr " + chunkNo + " for file " + fileId);

        if(!PeerThread.savedChunks.containsKey(fileId) ||
                !PeerThread.savedChunks.get(fileId).contains(chunkNo)) return;

        System.out.println("I have this chunk");

        byte[] body = FileManager.getChunk(fileId,chunkNo);

        Random generator = new Random();
        try {
            Thread.sleep(generator.nextInt(Utils.AVOID_CONCURRENCY_SLEEP_DURATION));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(!PeerThread.sentChunks.containsKey(fileId) ||
                !PeerThread.sentChunks.get(fileId).contains(chunkNo)) {
            System.out.println("Sending Chunk");
            Message msg = new Message(Message.ANS_RESTORE, headerParams[VERSION], PeerThread.serverID,
                    headerParams[FILE_ID], headerParams[CHUNK_NO], body, body.length);
            msg.sendMessage(PeerThread.restoreThread.channelSocket, PeerThread.restoreThread.address, PeerThread.restoreThread.port);
        }
        else
            System.out.println("Chunk already sent");
    }

    private void processRemoved(String[] headerParams) {
        int serverId = Integer.parseInt(headerParams[SENDER_ID]);
        String fileId = headerParams[FILE_ID];
        int chunkNo = Integer.parseInt(headerParams[CHUNK_NO]);

        System.out.println("Server " + headerParams[SENDER_ID] + " deleted chunk " + chunkNo + " for file " + fileId);

        if(PeerThread.serversContaining.contains(fileId) && PeerThread.serversContaining.get(fileId).contains(chunkNo))
            PeerThread.serversContaining.get(fileId).get(chunkNo).remove(serverId);

        if(!PeerThread.savedChunks.contains(fileId) || !PeerThread.savedChunks.get(fileId).contains(chunkNo))
            return;

        if(PeerThread.serversContaining.get(fileId).get(chunkNo).size() < PeerThread.desiredFileReplication.get(fileId)){
            System.out.println("Initiating backup for chunk...");
            byte[] chunk = FileManager.getChunk(fileId, chunkNo);
            if(!PeerThread.chunksToBackup.contains(fileId))
                PeerThread.chunksToBackup.put(fileId, new HashSet<>());
            PeerThread.chunksToBackup.get(fileId).add(chunkNo);
            try {
                Thread.sleep(new Random().nextInt(Utils.AVOID_CONCURRENCY_SLEEP_DURATION));
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
            if(PeerThread.chunksToBackup.contains(fileId) && PeerThread.chunksToBackup.get(fileId).contains(chunkNo)) {
                BackupProtocol.backupChunk(chunk, chunk.length, PeerThread.desiredFileReplication.get(fileId), fileId, chunkNo);
                PeerThread.chunksToBackup.get(fileId).remove(chunkNo);
                System.out.println("Backup successfully initiated");
            }
            else {
                System.out.println("Backup was already initiated by another peer");
            }
        }

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

        switch (messageParams[0]) {
            case Message.ANS_BACKUP:
                processStored(messageParams);
                break;
            case Message.INIT_DELETE:
                try {
                    FileManager.deleteFile(messageParams[FILE_ID]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case Message.INIT_RESTORE:
                processGetChunk(messageParams);
                break;
            case Message.RECLAIM:
                processRemoved(messageParams);
                break;
        }

    }

}
