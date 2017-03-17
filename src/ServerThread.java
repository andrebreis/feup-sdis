import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

/**
 * Created by chrx on 2/24/17.
 */
public class ServerThread extends Thread {


    protected ServerSocket serverSocket = null;
    private HashMap<String,String> licensePlates;


    public ServerThread(int serverPort) throws IOException {
        this("Server", serverPort);
    }

    public ServerThread(String name, int serverPort) throws IOException {
        super(name);

        serverSocket = new ServerSocket(serverPort);

        licensePlates = new HashMap<>();

    }

    private class ServerProtocol extends Thread {

        private Socket clientSocket;
        private HashMap<String,String> licensePlates;


        public ServerProtocol(Socket clientSocket, HashMap<String,String> licensePlates){
            this.clientSocket = clientSocket;
            this.licensePlates = licensePlates;
        }

        private String processCommand(String command){
            String[] params = command.split(" ");

            if(params[0].equals("register")){
                if(params.length != 3)
                    return "Wrong Number of parameters for register";
                if(!Pattern.matches("\\w\\w-\\w\\w-\\w\\w", params[1]))
                    return "-1";
                if(licensePlates.containsKey(params[1]))
                    return "-1";

                licensePlates.put(params[1],params[2]);
                return Integer.toString(licensePlates.size());
            }
            else if(params[0].equals("lookup")){
                if(params.length != 2)
                    return "Wrong Number of parameters for lookup";
                if(!licensePlates.containsKey(params[1]))
                    return "NOT_FOUND";
                return licensePlates.get(params[1]);
            }
            return "UNKNOWN_COMMAND";
        }

        public void run() {
            try {
                PrintWriter out =
                        new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));

                String inputLine, outputLine;
                while ((inputLine = in.readLine()) != null) {
                    outputLine = processCommand(inputLine);
                    out.println(outputLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void run() {

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                new ServerProtocol(clientSocket, licensePlates).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        socket.close();
    }


}
