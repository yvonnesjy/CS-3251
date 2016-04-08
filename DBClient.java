import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public class dbclientTCP {
    public static void main(String args[]) throws IOException {
        StringTokenizer ipPort = new StringTokenizer(args[0], ":"); //Tokenizer to split the ip and port
        // - I know I can use String.split() but I had already done this and don't feel like redoing it

        InetAddress ip = InetAddress.getByName(ipPort.nextToken()); //The IP of the server to connect to
        int port = Integer.parseInt(ipPort.nextToken()); //The port of the server to connect to
        String output = ""; //String to hold our request to the server
        String input; //String to hold the server's response
        byte[] outputData;
        byte[] inputData;

        try {
            //Setup connection to server
            RTPSocket clientSocket = new RTPSocket(ip, port);
            //Create our request String
            for (int i = 1; i < args.length; i++) {
                output += args[i] + " ";
            }
            //Send the request
            outputData = output.getBytes();
            clientSocket.writeBytes(outputData);
            //Read and output response, terminate connection
            inputData = clientSocket.readBytes();
            input = new String(inputData);
            System.out.println("From Server: " + input);
            clientSocket.close();
        } catch (ConnectException refused) {
            System.out.println("Error: could not connect to server.");
        }
    }
}
