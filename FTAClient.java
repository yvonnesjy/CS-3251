import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.lang.System;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class FTAClient {

    public static void main (String[] args) throws IOException {
        //Make sure we have only our 2 arguements
        if (args.length != 2) {
            throw new IllegalArgumentException("Invalid arguments");
        }

        //Parse the hostname/IP and port #
        String[] HP = args[0].split(":");
        if (HP.length != 2){
            throw new IllegalArgumentException("Invalid Address:Port");
        }

        InetAddress ip;
        try {
            ip = InetAddress.getByName(HP[0]); //The IP of the server to connect to
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid IP address");
        }

        int port;
        try {
            port = Integer.parseInt(HP[1]); //The port of the server to connect to
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port number");
        }

        int sizeW;
        try {
            sizeW = Integer.parseInt(args[1]); //The port of the server to connect to
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid window size");
        }
        //TODO: make a method that sets up a connection with the server.
        //something like RTP clientConnection = new RTP(ip, port, sizeW);

        System.out.println("Connection Established.");

        Scanner commands = new Scanner(System.in);
        boolean disconnect = false;
        while(!disconnect) {
            System.out.println("Enter a command:");
            String command = commands.nextLine();
            String[] pieces = command.split(" ");
            if (pieces[0].equals("get")) {
                if (pieces.length != 2) {
                    throw new IllegalArgumentException("Invalid arguments for command \"get\".");
                }

                //tell the server we're doing a get
                clientConnection.write(command.getBytes());

                //receive the file
                byte[] newFileData = clientConnection.read();
                FileOutputStream stream = new FileOutputStream(Paths.get("get_F"));
                try {
                    stream.write(newFileData);
                } finally {
                    stream.close();
                }
                System.out.println("File received.");

            } else if (pieces[0].equals("get-post")) {
                if (pieces.length != 3) {
                    throw new IllegalArgumentException("Invalid arguments for command \"get-post\".");
                }
                //Load our file into a byte array
                Path path = Paths.get(pieces[1]);
                byte[] file = Files.readAllBytes(path);

                //tell the server we're doing a get-post
                clientConnection.write(command.getBytes());

                //send the file
                clientConnection.write(file);
                String request = "get " + Filename
                System.out.println("File sent.");

                //receive the file
                byte[] newFileData = clientConnection.read();
                FileOutputStream stream = new FileOutputStream(Paths.get("get_F"));
                try {
                    stream.write(newFileData);
                } finally {
                    stream.close();
                }
                System.out.println("File received.");

            } else if (pieces[0].equals("disconnect")) {
                if (pieces.length != 1) {
                    throw new IllegalArgumentException("Invalid arguments for command \"disconnect\".");
                }

                disconnect = true;
            } else {
                System.out.println("Invalid command.");
            }
        }

        //TODO: make a method to close the connection.
        clientConnection.close();
        System.out.println("Connection Terminated.");

    }
}