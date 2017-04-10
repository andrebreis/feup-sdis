package protocols;

import channels.Message;
import file_manager.FileManager;
import file_manager.Utils;
import server.PeerThread;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by chrx on 4/10/17.
 */
public class RestoreProtocol {

    final private static int MAX_CHUNKS_PER_REQUEST = 10;
    private static ExecutorService threadPool;

    private static boolean receivedAllChunks(String fileId) {
        byte[] lastChunk = PeerThread.restoringChunks.get(fileId).get(PeerThread.restoringChunks.get(fileId).size());
        return lastChunk != null && lastChunk.length < FileManager.MAX_CHUNK_SIZE;
    }

    private static void restoreChunk(String fileId, int chunkNo) {
        threadPool.submit(
                new Thread(() -> {
                    Message getChunk = new Message(Message.INIT_RESTORE, "1.0", PeerThread.serverID, fileId, Integer.toString(chunkNo));
                    do {
                        getChunk.sendMessage(PeerThread.controlThread.getChannelSocket(), PeerThread.controlThread.getAddress(), PeerThread.controlThread.getPort());
                        try {
                            Thread.sleep(3 * Utils.SECONDS_TO_MILLISECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    while (!(PeerThread.restoringChunks.get(fileId).containsKey(chunkNo) || receivedAllChunks(fileId)));
                }
                ));
    }

    public static void restoreFile(String filepath) {

        String fileId = PeerThread.fileIds.get(filepath);

        int chunkNo = 1;
        PeerThread.restoringChunks.put(fileId, new ConcurrentHashMap<>());
        
        do {
            threadPool = Executors.newFixedThreadPool(MAX_CHUNKS_PER_REQUEST);
            for (int i = chunkNo; i < chunkNo + MAX_CHUNKS_PER_REQUEST; i++) {
                restoreChunk(fileId, i);
            }
            chunkNo += MAX_CHUNKS_PER_REQUEST;
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(MAX_CHUNKS_PER_REQUEST / 2, TimeUnit.SECONDS)) {
                    do {
                        threadPool.shutdownNow();
                    } while (!threadPool.isTerminated());
                    return;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        } while (!receivedAllChunks(fileId));

        FileManager.mergeFile(filepath);
    }


}
