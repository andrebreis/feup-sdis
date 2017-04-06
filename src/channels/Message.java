package channels;

/**
 * Created by ines on 06-04-2017.
 */
public class Message {

    String headerString;
    byte[] message;

    public Message(String messageType, String version, String senderId,
                   String fileId, int chunkNo, int replicationDegree, byte[] body, int bodyLength){

        headerString = messageType + " " + version + " " + senderId + " " + fileId;

        if(chunkNo != -1) headerString += " " + Integer.toString(chunkNo);
        if(replicationDegree != -1) headerString += " " + Integer.toString(replicationDegree);

        headerString += "\r\n\r\n";

        byte[] headerBytes = headerString.getBytes();
        message = new byte[headerBytes.length + bodyLength];
        System.arraycopy(headerBytes, 0, message, 0, headerBytes.length);
        System.arraycopy(body,0, message, headerBytes.length, bodyLength);
    }

    public Message(String messageType, String version, String senderId, String fileId, int chunkNo, byte[] body, int bodyLength){
        this(messageType,version,senderId,fileId,chunkNo,-1,body, bodyLength);
    }

    public Message(String messageType, String version, String senderId, String fileId, int chunkNo){
        this(messageType,version,senderId,fileId,chunkNo, new byte[]{},0);
    }

    public Message(String messageType, String version, String senderId, String fileId){
        this(messageType,version,senderId,fileId,-1, new byte[]{},0);
    }



}
