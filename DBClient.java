import java.net.*;
import java.io.*;

public class DBClient {
    public static void main(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("Invalid arguments");
        }

        InetAddress ip;
        int port;
        boolean fn = false, ln = false, point = false, hours = false, gpa = false;
        String id;
        
        String[] addr = args[0].split(":");
        if (addr.length != 2) {
            throw new IllegalArgumentException("Invalid address");
        }

        try {
            ip = InetAddress.getByName(addr[0]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid IP address");
        }

        try {
            port = Integer.valueOf(addr[1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid port number");
        }

        id = args[1];

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

        try {
            clientRTP(ip, port, id, fn, ln, point, hours, gpa);
        } catch (SocketException e) {
            System.err.println("SocketException: " + e.getMessage());
        }
    }

    public static void clientRTP(InetAddress ip, int port, String id, boolean fn, boolean ln, boolean point, boolean hours, boolean gpa) throws SocketException {
        DatagramSocket clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(10000);

        String msg = "00010"; // seq# = 0, ack#, ACK = 0, SYN = 1, FIN = 0
        byte[] sendData = msg.getBytes();
        byte[] receiveData = new byte[Util.PACKETSIZE];
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
        DatagramPacket receivePacket = null;
        boolean ioFailure;

        do {
            ioFailure = false;
            try {
                clientSocket.send(sendPacket);
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);
            } catch (IOException e) {
                ioFailure = true;
            }
        } while (ioFailure);

        String content;
        InetAddress serverIP, connectionIP = null;
        int serverPort, connectionPort;
        boolean corruptedData;
        do {
            corruptedData = false;
            content = new String(receivePacket.getData());
            serverIP = receivePacket.getAddress();
            serverPort = receivePacket.getPort();

            String data = content.substring(Util.DATA_IND);
            String[] addr = data.split(":");
            if (addr.length != 2) {
                corruptedData = true;
                continue;
            }

            try {
                connectionIP = InetAddress.getByName(addr[0]);
            } catch (UnknownHostException e) {
                corruptedData = true;
                continue;
            }
            connectionPort = Integer.valueOf(addr[1].trim());

            String ackNum, seqNum;
            try {
                ackNum = Util.getAckNum(content);
                seqNum = Util.getSeqNum(content);
            } catch (Exception e) {
                corruptedData = true;
                continue;
            }

            // seq#, ack#, ACK, SYN = 1, FIN
            msg = seqNum + ackNum + "010";

            sendData = msg.getBytes();
            sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
            do {
                ioFailure = false;
                try {
                    clientSocket.send(sendPacket);
                } catch (IOException e) {
                    ioFailure = true;
                }
            } while (ioFailure);

            msg = "00000" + id;
            clientSocket.connect(connectionIP, connectionPort);
            String info = TCPQuery(clientSocket, connectionIP, connectionPort, msg, fn, ln, point, hours, gpa);
            try {
                Util.printInfo(info, fn, ln, point, hours, gpa);
            } catch (Exception e) {
                clientSocket.disconnect();
                corruptedData = true;
            }

        } while (corruptedData || content.charAt(Util.ACK) != '1'
            || !content.substring(0, Util.ACK_NUM_IND + Util.ACK_NUM_OFFSET).equals("1")
            || content.charAt(Util.SYN) != '1'
            || !serverIP.equals(ip) || serverPort != port);
        // isAck, ACK# = 1, serverIP == ip, serverPort == port, correct data
        
        clientSocket.close();
    }

    public static String TCPQuery(DatagramSocket clientSocket, InetAddress connectionIP, int connectionPort, String msg, boolean fn, boolean ln, boolean point, boolean hours, boolean gpa) {
        byte[] sendData = msg.getBytes();
        byte[] receiveData = new byte[Util.PACKETSIZE];
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, connectionIP, connectionPort);
        DatagramPacket receivePacket;
        String content = null;
        boolean ioFailure;
        do {
            ioFailure = false;
            try {
                clientSocket.send(sendPacket);
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);
                content = new String(receivePacket.getData());
            } catch (IOException e) {
                System.err.println(e.getMessage());
                ioFailure = true;
            }
        } while (ioFailure);

        return content.substring(Util.DATA_IND);
    }
}