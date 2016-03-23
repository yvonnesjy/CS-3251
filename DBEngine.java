import java.util.List;
import java.util.Arrays;
import java.net.*;
import java.io.*;

public class DBEngine {
    static List<String> ids = Arrays.asList("903076259", "903084074", "903077650", "903083691", "903082265", "903075951", "903084336", "902987866", "000000000");
    static String[] fn = {"Anthony", "Richard", "Joe", "Todd", "Laura", "Marie", "Stephen", "Yvonne", "Bud"};
    static String[] ln = {"Peterson", "Harris", "Miller", "Collins", "Stewart", "Cox", "Baker", "Shi", "Peterson"};
    static int[] points = {231, 236, 224, 218, 207, 246, 234, 300, 100};
    static int[] hours = {63, 66, 65, 56, 64, 63, 66, 77, 100};

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Invalid arguments");
        }

        int port;
        try {
            port = Integer.valueOf(args[0]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid port number");
        }

        serverRTP(port);
    }

    public static void serverRTP(int port) {
        // try {
        //     DatagramSocket serverSocekt = new DatagramSocket(port);
        //     System.out.println("The server is ready to receive");
        //     byte[] receiveData = new byte[1024];
        //     byte[] sendData = new byte[1024];

        //     while (true) {
        //         DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        //         serverSocket.receive(receivePacket);

        //         byte[] id = receivePacket.getData();
        //         InetAddress clientAddr = receivePacket.getAddress();
        //         int clientPort = receivePacket.getPort();

                
        //     }
        // }
    }
}