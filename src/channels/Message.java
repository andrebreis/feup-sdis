package channels;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by ines on 06-04-2017.
 */
public class Message {

    private String headerString;
    private byte[] messageBytes;

    public Message(String messageType, String version, int senderId,
                   String fileId, String chunkNo, int replicationDegree, byte[] body, int bodyLength){

        headerString = messageType + " " + version + " " + senderId + " " + fileId;

        if(!chunkNo.equals("-1")) headerString += " " + chunkNo;
        if(replicationDegree != -1) headerString += " " + Integer.toString(replicationDegree);

        headerString += "\r\n\r\n";

        byte[] headerBytes = headerString.getBytes();
        messageBytes = new byte[headerBytes.length + bodyLength];
        System.arraycopy(headerBytes, 0, messageBytes, 0, headerBytes.length);
        System.arraycopy(body,0, messageBytes, headerBytes.length, bodyLength);
    }

    public Message(String messageType, String version, int senderId,
                   String fileId, int chunkNo, int replicationDegree, byte[] body, int bodyLength){
        this(messageType, version, senderId, fileId, Integer.toString(chunkNo), replicationDegree, body, bodyLength);
    }

    public Message(String messageType, String version, int senderId, String fileId, String chunkNo, byte[] body, int bodyLength){
        this(messageType,version,senderId,fileId,chunkNo,-1,body, bodyLength);
    }

    public Message(String messageType, String version, int senderId, String fileId, String chunkNo){
        this(messageType,version,senderId,fileId,chunkNo, new byte[]{},0);
    }

    public Message(String messageType, String version, int senderId, String fileId){
        this(messageType,version,senderId,fileId,"-1", new byte[]{},0);
    }

    public byte[] getBytes() {
        return messageBytes;
    }

    public void sendMessage(MulticastSocket socket, InetAddress address, int port){
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, address, port);

        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageWithDelay(MulticastSocket socket, InetAddress address, int port) {
        Random generator = new Random();

        Executors.newSingleThreadScheduledExecutor().schedule(
                () -> sendMessage(socket, address, port),
                generator.nextInt(401),
                TimeUnit.MILLISECONDS

        );

    }

    public static int getHeaderLength(byte[] message, int length) {
        for (int i = 3; i < length; i++) {
            if (message[i - 3] == '\r' && message[i - 2] == '\n'
                    && message[i - 1] == '\r' && message[i] == '\n')
                return i+1; // i is index, i+1 is length
        }
        return -1;
    }



}
