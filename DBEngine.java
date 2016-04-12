import java.util.*;
import java.net.*;
import java.io.*;

public class DBEngine {
    final static int TIMEOUT = 1000;

    static List<String> ids = Arrays.asList("903076259", "903084074", "903077650", "903083691", "903082265", "903075951", "903084336", "902987866", "000000000");
    static String[] fn = {"Anthony", "Richard", "Joe", "Todd", "Laura", "Marie", "Stephen", "Yvonne", "Bud"};
    static String[] ln = {"Peterson", "Harris", "Miller", "Collins", "Stewart", "Cox", "Baker", "Shi", "Peterson"};
    static int[] points = {231, 236, 224, 218, 207, 246, 234, 300, 100};
    static int[] hours = {63, 66, 65, 56, 64, 63, 66, 77, 100};

    static Hashtable<InetSocketAddress, List<String>> inbox;

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

        inbox = new Hashtable<>();

        try {
            serverRTP(port);
        } catch (SocketException e) {
            System.err.println("SocketException: " + e.getMessage());
        }
    }

    private static void serverRTP(int port) throws SocketException {
        DatagramSocket serverSocket = new DatagramSocket(port);
        System.out.println("The server is ready to receive");

        while (true) {
            DatagramPacket receivePacket = RTP.makeReceivePacket();
            try {
                serverSocket.receive(receivePacket);
            } catch (IOException e) {
                break;
            }
            InetSocketAddress client = Multiplexer.multiplex(receivePacket);

            if (client != null) {
                new Thread(new connectionThread(serverSocket, client)).start();
            }
        }
    }

    private static class connectionThread implements Runnable {
        private DatagramSocket serverSocket;
        private InetSocketAddress clientAddr;
        private RTP rtpService;

        public connectionThread(DatagramSocket serverSocket, InetSocketAddress client) {
            this.serverSocket = serverSocket;
            clientAddr = client;
            rtpService = new RTP();
        }

        public void run() {
            String requestMsg = setUpConnection();
            if (requestMsg == null) return;
            String teardownMsg = respond(requestMsg);
            teardown(teardownMsg);
        }

        public String setUpConnection() {
            String msg = inbox.get(clientAddr).remove(0);
            if (!rtpService.isSYN(msg) || rtpService.isACK(msg)) {
                inbox.remove(clientAddr);
                return null;
            }

            DatagramPacket sendPacket = rtpService.makeConnectionPacket(msg, clientAddr, "");
            int count = 0;
            boolean firstPacket = true;
            // TODO: check seq and ack
            while (inbox.get(clientAddr).isEmpty()
                || !rtpService.isInOrder(sendPacket, inbox.get(clientAddr).get(0))
                || !rtpService.isSYN(inbox.get(clientAddr).get(0))) {
                count++;
                if (!inbox.get(clientAddr).isEmpty()) {
                	inbox.get(clientAddr).remove(0);
                }
                if (firstPacket || count == TIMEOUT) {
                    while (true) {
                        try {
                            serverSocket.send(sendPacket);
                            firstPacket = false;
                            break;
                        } catch (IOException e) {
                            continue;
                        }
                    }
                    if (count == 1000) {
                        count = 0;
                    }
                }
            }
            String request = inbox.get(clientAddr).remove(0);
            System.out.printf("Connected with %s\n", clientAddr.toString());
            return request;
        }

        public String respond(String clientMsg) {
            String id = rtpService.getData(clientMsg);
            int ind = ids.indexOf(id.trim());
            String info;
            if (ind == -1) {
                info = "Invalid GTID";
            } else {
                info = fn[ind] + " " + ln[ind] + " " + points[ind] + " " + hours[ind];
            }

            DatagramPacket sendPacket = rtpService.makePackets(clientMsg, clientAddr, info).get(0);

            int count = 0;
            boolean firstPacket = true;

            while (inbox.get(clientAddr).isEmpty()
                || !rtpService.isInOrder(sendPacket, inbox.get(clientAddr).get(0))
                || !rtpService.isFIN(inbox.get(clientAddr).get(0))) {
                count++;
                if (!inbox.get(clientAddr).isEmpty()) {
                    inbox.get(clientAddr).remove(0);
                }
                if (firstPacket || count == TIMEOUT) {
                    while (true) {
                        try {
                            serverSocket.send(sendPacket);
                            firstPacket = false;
                            break;
                        } catch (IOException e) {
                            continue;
                        }
                    }
                    if (count == TIMEOUT) {
                    	count = 0;
                    }
                }
            }

            String teardown = inbox.get(clientAddr).remove(0);
            return teardown;
        }

        public void teardown(String clientMsg) {
            DatagramPacket sendPacket = rtpService.teardownPacket(clientMsg, clientAddr);
            while (true) {
                try {
                    serverSocket.send(sendPacket);
                    break;
                } catch (IOException e) {
                    continue;
                }
            }
        }
    }

    private static class Multiplexer {
        public static InetSocketAddress multiplex(DatagramPacket receivePacket) {
            InetSocketAddress addr;
            try {
                addr = (InetSocketAddress) receivePacket.getSocketAddress();
            } catch (IllegalArgumentException e) { // To filter some system internet configuration socket
                return null;
            }
            String rtpData = new String(receivePacket.getData());

            if (inbox.containsKey(addr)) {
            	List<String> packetQueue = inbox.get(addr);
            	if (packetQueue.isEmpty() || !packetQueue.get(packetQueue.size() - 1).equals(rtpData)) {
            		packetQueue.add(rtpData);
            	}
                return null;
            }

            List<String> packetQueue = new LinkedList<>();
            packetQueue.add(rtpData);
            inbox.put(addr, packetQueue);
            return addr;
        }
    }
}