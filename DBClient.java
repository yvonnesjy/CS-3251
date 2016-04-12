import java.io.*;
import java.net.*;
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

        clientRTP(ip, port, id, fn, ln, point, hours, gpa);
    }

    private static void clientRTP(InetAddress ip, int port, String id, boolean fn, boolean ln, boolean point, boolean hours, boolean gpa) throws SocketException {
        RTP rtpService = new RTP();
        DatagramSocket clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(TIMEOUT);
        InetSocketAddress serverAddr = new InetSocketAddress(ip, port);

        String connectionConfirmMsg = setUpConnection(clientSocket, serverAddr, rtpService);
        String infoMsg = requestInfo(connectionConfirmMsg, clientSocket, serverAddr, id, rtpService, fn, ln, point, hours, gpa);
        teardown(infoMsg, clientSocket, serverAddr, rtpService);
    }

    private static String setUpConnection(DatagramSocket clientSocket, InetSocketAddress serverAddr, RTP rtpService) {
        DatagramPacket sendPacket = rtpService.makeConnectionPacket(null, serverAddr, "");
        DatagramPacket receivePacket = RTP.makeReceivePacket();

        while(true) {
            try {
                clientSocket.send(sendPacket);
            } catch (IOException e) {
                continue;
            }

            boolean pass = false;

            while (true) {
                try {
                    clientSocket.receive(receivePacket);
                } catch (SocketTimeoutException e) {
                    break; // resend
                } catch (IOException e) {
                    continue;
                }
                String msg = new String(receivePacket.getData());
                // TODO: check seq and ack
                if (!receivePacket.getSocketAddress().equals(serverAddr)
                    || !rtpService.isInOrder(sendPacket, msg)
                    || !rtpService.isSYN(msg)) {
                    continue;
                }
                pass = true;
                break;
            }
            if (pass) {
                break;
            }
        }

        return new String(receivePacket.getData());
    }

    private static String requestInfo(String serverMsg, DatagramSocket clientSocket, InetSocketAddress serverAddr, String id, RTP rtpService,
            boolean fn, boolean ln, boolean point, boolean hours, boolean gpa) {
        DatagramPacket sendPacket = rtpService.makeConnectionPacket(serverMsg, serverAddr, id);
        DatagramPacket receivePacket = RTP.makeReceivePacket();

        while(true) {
            try {
                clientSocket.send(sendPacket);
            } catch (IOException e) {
                continue;
            }

            boolean pass = false;
            int count = -1;
            while (true) {
            	count++;
            	if (count == TIMEOUT) {
            		break;
            	}
                try {
                    clientSocket.receive(receivePacket);
                } catch (SocketTimeoutException e) {
                    break; // resend
                } catch (IOException e) {
                    continue;
                }
                String msg = new String(receivePacket.getData());

                // TODO: check seq and ack
                if (!receivePacket.getSocketAddress().equals(serverAddr)
                    || !rtpService.isInOrder(sendPacket, msg)) {
                    continue;
                }
                pass = true;
                break;
            }

            String msg = new String(receivePacket.getData());
            if (pass) {
                String info = rtpService.getData(msg);
                try {
                    printInfo(info, fn, ln, point, hours, gpa);
                } catch (Exception e) {
                    continue;
                }
                break;
            }
        }

        return new String(receivePacket.getData());
    }

    private static void teardown(String serverMsg, DatagramSocket clientSocket, InetSocketAddress serverAddr, RTP rtpService) {
        DatagramPacket sendPacket = rtpService.teardownPacket(serverMsg, serverAddr);
        while (true) {
            try {
                clientSocket.send(sendPacket);
                break;
            } catch (IOException e) {
                continue;
            }
        }
    }

    private static void printInfo(String infoRec, boolean fn, boolean ln, boolean point, boolean hours, boolean gpa) throws Exception {
        String[] info = infoRec.split(" ");
        if (info.length != 4) {
            throw new Exception();
        }

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





















