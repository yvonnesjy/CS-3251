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

        try {
            serverRTP(port);
        } catch (SocketException e) {
            System.err.println("SocketException: " + e.getMessage());
        } catch (UnknownHostException e) {
            System.err.println("UnknownHostException: " + e.getMessage());
        }
    }

    public static void serverRTP(int port) throws SocketException, UnknownHostException {
        DatagramSocket serverSocket = new DatagramSocket(port);
        // serverSocket.setSoTimeout(10000);

        System.out.println("The server is ready to receive");

        while (true) {
            byte[] receiveData = new byte[Util.PACKETSIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            boolean ioFailure;
            do {
                ioFailure = false;
                try {
                    serverSocket.receive(receivePacket);
                } catch (IOException e) {
                    ioFailure = true;
                    continue;
                }
            } while (ioFailure);

            String msg = new String(receivePacket.getData());
            InetAddress clientAddr = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();
            if (msg.charAt(Util.SYN) != '1') {
                continue;
            }

            try {
                // Ack = 1, SYN = 1, FIN = 0;
                msg = Util.getSeqNum(msg) + Util.getAckNum(msg) + "110";
            } catch (Exception e) {
                continue;
            }

            DatagramSocket connectionSocket = null;
            do {
                try {
                    connectionSocket = new DatagramSocket();
                    connectionSocket.connect(clientAddr, clientPort);
                    connectionSocket.setSoTimeout(10000);
                } catch (SocketException e) {
                }
            } while (connectionSocket == null);

            // Inet4Address.getLocalHost().getHostAddress()
            msg += "127.0.0.1:" + connectionSocket.getLocalPort();

            byte[] sendData = msg.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddr, clientPort);
            do {
                ioFailure = false;
                try {
                    serverSocket.send(sendPacket);
                } catch (IOException e) {
                    ioFailure = true;
                }
            } while (ioFailure);

            new Thread(new connectionThread(connectionSocket, clientAddr, clientPort)).start();
            connectionSocket.close();
        }
    }

    private static class connectionThread implements Runnable {
        private DatagramSocket connectionSocket;
        private InetAddress clientAddr;
        private int clientPort;

        public void run() {
            byte[] receiveData = new byte[Util.PACKETSIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            boolean ioFailure;
            do {
                ioFailure = false;
                try {
                    connectionSocket.receive(receivePacket);
                } catch (IOException e) {
                    ioFailure = true;
                    continue;
                }
            } while (ioFailure);

            String content = new String(receivePacket.getData());
            int ind = ids.indexOf(content.substring(Util.DATA_IND).trim());
            String info;
            if (ind == -1) {
                info = "Invalid GTID";
            } else {
                info = fn[ind] + " " + ln[ind] + " " + points[ind] + " " + hours[ind];
            }
            byte[] sendData = info.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddr, clientPort);

            do {
                ioFailure = false;
                try {
                    connectionSocket.send(sendPacket);
                } catch (IOException e) {
                    ioFailure = true;
                    continue;
                }
            } while (ioFailure);




            // byte[] sendData = "ha".getBytes();
            // DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddr, clientPort);
            // boolean ioFailure;
            // do {
            //     ioFailure = false;
            //     try {
            //         connectionSocket.send(sendPacket);
            //     } catch (IOException e) {
            //         ioFailure = true;
            //     }
            // } while (ioFailure);
        }

        public connectionThread(DatagramSocket connectionSocket, InetAddress clientAddr, int clientPort) {
            this.connectionSocket = connectionSocket;
            this.clientAddr = clientAddr;
            this.clientPort = clientPort;
        }
    }
}