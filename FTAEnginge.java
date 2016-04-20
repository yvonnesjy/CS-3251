import javax.xml.crypto.Data;
import java.lang.Runnable;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Queue;

public class FTAEngine {
    public HashMap<InetAddress, Queue<DatagramPacket>> packets;

    public static void main (String[] args) throws IOException {
        //Make sure we have only our 2 arguements
        if (args.length != 2) {
            throw new IllegalArgumentException("Invalid arguments");
        }

        int port;
        try {
            port = Integer.parseInt(args[0]); //The port of the server to connect to
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port number");
        }

        int sizeW;
        try {
            sizeW = Integer.parseInt(args[1]); //The port of the server to connect to
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid window size");
        }

        try {
            packets = new HashMap<InetAddress, Queue<DatagramPacket>>();
            RTP serverConnection = new serverConnection(port, sizeW);
        } catch (SocketException e) {
            System.err.println("SocketException: " + e.getMessage());
        }



    }

    private class connectionThread implements Runnable {
        private FTAEngine server;
        private InetAddress client;
        private Queue<DatagramPacket> myPackets;

        public connectionThread(InetAddress client, FTAEngine server) {
            this.server = server;
            this.client = client;
            myPackets = packets.get(client);
        }

        public void run(){

            boolean disconnect = false;
            while(!disconnect) {
                //get command from the client
                String command = new String(serverConnection.read(myPackets));
                String[] pieces = command.split(" ");
                if (pieces[0].equals("get")) {
                    if (pieces.length != 2) {
                        throw new IllegalArgumentException("Invalid arguments for command \"get\".");
                    }

                    //Load our file into a byte array
                    Path path = Paths.get(pieces[1]);
                    byte[] file = Files.readAllBytes(path);

                    //send the file
                    serverConnection.write(file, client);
                    System.out.println("File sent.");

                } else if (pieces[0].equals("get-post")) {
                    if (pieces.length != 3) {
                        throw new IllegalArgumentException("Invalid arguments for command \"get-post\".");
                    }
                    //Load our file into a byte array
                    Path path = Paths.get(pieces[1]);
                    byte[] file = Files.readAllBytes(path);

                    //receive the file
                    byte[] newFileData = clientConnection.read(myPackets);
                    FileOutputStream stream = new FileOutputStream(Paths.get("post_G"));
                    try {
                        stream.write(newFileData);
                    } finally {
                        stream.close();
                    }
                    System.out.println("File received.");

                    //send the file
                    clientConnection.write(file, client);
                    System.out.println("File sent.");

                } else if (pieces[0].equals("disconnect")) {
                    if (pieces.length != 1) {
                        throw new IllegalArgumentException("Invalid arguments for command \"disconnect\".");
                    }

                    disconnect = true;
                } else {
                    System.out.println("Invalid command.");
                }
            }
        }


    }


}