import java.net.*;

public class DBClient {
    public static void main(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("Invalid arguments");
        }

        InetAddress ip;
        int port, fn = 0, ln = 0, point = 0, hours = 0, gpa = 0;
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
                fn = 1;
            } else if (args[i].equals("last_name")) {
                ln = 1;
            } else if (args[i].equals("quality_points")) {
                point = 1;
            } else if (args[i].equals("gpa_hours")) {
                hours = 1;
            } else if (args[i].equals("gpa")) {
                gpa = 1;
            } else {
                throw new IllegalArgumentException("Invalid query type");
            }
        }

        System.out.println("ip: " + ip + " port: " + port + " fn: " + fn + " ln: " + ln + " point: " + point + " hours: " + hours + " gpa: " + gpa);
        clientRTP(ip, port, id, fn, ln, point, hours, gpa);
    }

    public static void clientRTP(InetAddress ip, int port, String id, int fn, int ln, int point, int hours, int gpa) {
        boolean socketFailed;
        do {
            socketFailed = false;
            try {
                DatagramSocket clientSocket = new DatagramSocket();
                String msg = "00010"; // SEQ# = 0, SYN = 1
                byte[] sendData = msg.getBytes();
                byte[] receiveData = new byte[Util.PACKETSIZE];
                String content;
                InetAddress serverIP, connectionIP = null;
                int serverPort, connectionPort = 0;
                boolean corruptedData;
                do {
                    corruptedData = false;
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
                    clientSocket.send(sendPacket);
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    content = new String(receivePacket.getData());
                    serverIP = receivePacket.getAddress();
                    serverPort = receivePacket.getPort();
                    try {
                        String data = content.substring(Util.DATA_IND);

                        String[] addr = data.split(":");
                        if (addr.length != 2) {
                            corruptedData = true;
                        } else {
                            try {
                                connectionIP = InetAddress.getByName(addr[0]);
                            } catch (Exception e) {
                                corruptedData = true;
                            }

                            try {
                                connectionPort = Integer.valueOf(addr[1]);
                            } catch (Exception e) {
                                corruptedData = true;
                            }
                        }
                        try {
                            String ackNum = Util.getAckNum(content);
                            msg = "1" + ackNum + "010" + id + fn + "" + ln + "" + point + "" + hours + "" + gpa;
                        } catch (Exception e) {
                            corruptedData = true;
                        }
                    } catch (Exception e) {
                        corruptedData = true;
                    }
                } while (content.charAt(Util.ACK_NUM_IND) == '1' && content.substring(0, Util.ACK_NUM_IND + Util.ACK_NUM_OFFSET).equals("1") && serverIP.equals(ip) && serverPort == port && !corruptedData);
                // isAck, ACK# = 1, serverIP == ip, serverPort == port, correct data
                if (!corruptedData) {
                    do {
                        corruptedData = false;
                        String info = TCPQuery(clientSocket, connectionIP, connectionPort, msg);
                        try {
                            Util.printInfo(info, fn == 1, ln == 1, point == 1, hours == 1, gpa == 1);
                        } catch (Exception e) {
                            corruptedData = true;
                        }
                    } while (corruptedData);
                }
            } catch (Exception e) {
                socketFailed = true;
            }
        } while (socketFailed);
    }

    public static String TCPQuery(DatagramSocket clientSocket, InetAddress connectionIP, int connectionPort, String msg) {
        do {
            try {
                byte[] receiveData = new byte[Util.PACKETSIZE];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                String content = new String(receivePacket.getData());
                InetAddress serverIP = receivePacket.getAddress();
                int serverPort = receivePacket.getPort();
                if (serverIP.equals(connectionIP) && serverPort == connectionPort) {
                    byte[] sendData = msg.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverIP, serverPort);
                    clientSocket.send(sendPacket);
                    receiveData = new byte[Util.PACKETSIZE];
                    receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    content = new String(receivePacket.getData());
                    serverIP = receivePacket.getAddress();
                    serverPort = receivePacket.getPort();
                    if (serverIP.equals(connectionIP) && serverPort == connectionPort) {
                        return content.substring(Util.DATA_IND);
                    }
                }
            } catch (Exception e) {
            }
        } while (true);
    }
}