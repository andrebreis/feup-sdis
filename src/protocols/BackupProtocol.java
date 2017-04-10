package protocols;

import channels.Message;
import file_manager.Utils;
import server.PeerThread;

import java.util.concurrent.Executors;

/**
 * Created by chrx on 4/10/17.
 */
public class BackupProtocol {

    public static void backupChunk(byte[] chunk, int chunkSize, int replicationDegree, String fileId, int chunkNo) {
        Executors.newSingleThreadExecutor().execute(
                () -> {
                    int noSentCommands = 0;

                    Message putChunk = new Message(Message.INIT_BACKUP, "1.0", PeerThread.serverID, fileId, chunkNo, replicationDegree, chunk, chunkSize);
                    while (noSentCommands < Utils.NO_BACKUP_TRIES && PeerThread.getCurrentReplication(fileId, chunkNo) < replicationDegree) {
                        putChunk.sendMessage(PeerThread.backupThread.getChannelSocket(), PeerThread.backupThread.getAddress(), PeerThread.backupThread.getPort());
                        try {
                            Thread.sleep(2 ^ noSentCommands * Utils.SECONDS_TO_MILLISECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Sending PUTCHUNK no " + Integer.toString(noSentCommands) + "for chunk no " + Integer.toString(chunkNo));
                        noSentCommands++;
                    }
                    if (noSentCommands == Utils.NO_BACKUP_TRIES)
                        System.out.println("Couldn't backup chunk nr " + chunkNo + " for file " + fileId + " appropriately");
                    else
                        System.out.println("Backup for chunk nr " + chunkNo + " for file " + fileId + " finished successfully");
                }
        );
    }

}
