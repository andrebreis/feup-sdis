import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

/**
 * Created by chrx on 2/24/17.
 */
public class Client {

    public static void main(String[] args) throws IOException {

        if (args.length < 4) {
            System.out.println("java Client <host_name> <port_number> <oper> <opnd> * ");
            return;
        }


        Socket socket = new Socket(args[0], Integer.parseInt(args[1]));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

        // send request
        String cmd = "";
        for(int i = 2; i < args.length; i++)
            cmd += args[i] + " ";

        out.println(cmd);
        System.out.println(in.readLine());

    }

}
