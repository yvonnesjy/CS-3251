import java.io.*;
import java.lang.System;
import java.net.*;
import java.net.DatagramSocket;
import java.util.StringTokenizer;

public class DBClient {
    final static int TIMEOUT = 1000;

    public static void main(String args[]) throws IOException {
        if (args.length < 3) {
            throw new IllegalArgumentException("Invalid arguments");
        }

        StringTokenizer ipPort = new StringTokenizer(args[0], ":"); //Tokenizer to split the ip and port
        // - I know I can use String.split() but I had already done this and don't feel like redoing it

        InetAddress ip;
        try {
            ip = InetAddress.getByName(ipPort.nextToken()); //The IP of the server to connect to
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid IP address");
        }

        int port;
        try {
            port = Integer.parseInt(ipPort.nextToken()); //The port of the server to connect to
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port number");
        }

        String id = args[1];

        boolean fn = false, ln = false, point = false, hours = false, gpa = false;
        for (int i = 2; i < args.length; i++) {
            if (args[i].equals("first_name")) {
                fn = true;
            } else if (args[i].equals("last_name")) {
                ln = true;
            } else if (args[i].equals("quality_points")) {
                point = true;
            } else if (args[i].equals("gpa_hours")) {
                hours = true;
            } else if (args[i].equals("gpa")) {
                gpa = true;
            } else {
                throw new IllegalArgumentException("Invalid query type");
            }
        }

        RTP rtpService = new RTP(1);
        DatagramSocket clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(TIMEOUT);
        byte[] info = rtpService.clientRoutine(clientSocket, ip, port, id);
        printInfo(info, fn, ln, point, hours, gpa);
        rtpService.clientRoutine(clientSocket, ip, port, "disconnect");
        System.exit(0);
    }

    private static void printInfo(byte[] infoRec, boolean fn, boolean ln, boolean point, boolean hours, boolean gpa){
        String[] info = new String(infoRec).split(" ");

        System.out.print("From server:");
        if (fn) {
            System.out.print(" first_name: " + info[0]);
        }
        if (ln) {
            System.out.print(" last_name: " + info[1]);
        }
        if (point) {
            System.out.print(" quality_points: " + info[2]);
        }
        if (hours) {
            System.out.print(" gpa_hours: " + info[3]);
        }
        if (gpa) {
            System.out.format(" gpa: %.6f", Float.valueOf(info[2])/Float.valueOf(info[3]));
        }

        System.out.println();
    }
}





















