import java.io.*;
import java.lang.IllegalArgumentException;
import java.lang.System;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Scanner;

public class FTAClient {
    final static int TIMEOUT = 1000;

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

        RTP rtpService = new RTP(sizeW);

        Scanner scan = new Scanner(System.in);
        DatagramSocket clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(TIMEOUT);
        while (true) {
            String command = scan.nextLine();
            String[] components = command.split(" ");
            byte[] info = null;
            if (components[0].equals("get") && components.length == 2) {
                info = rtpService.clientRoutine(clientSocket, ip, port, components[1]);
            } else if (components[0].equals("disconnect") && components.length == 1) {
                rtpService.clientRoutine(clientSocket, ip, port, components[0]);
                break;
            } else {
                System.out.println("Invalid argument");
                continue;
            }
            if (info != null) {
                FileOutputStream stream = new FileOutputStream("get_F");
                try {
                    stream.write(info);
                } finally {
                    stream.close();
                }
            }
        }
        System.exit(0);
    }
}