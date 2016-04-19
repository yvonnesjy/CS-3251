import java.net.*;
import java.util.List;
import java.io.*;

public class ServerThread implements Runnable {
	final static int TIMEOUT = 1000;
	
    private DatagramSocket serverSocket;
    private InetSocketAddress clientAddr;
    private RTP rtpService;

    public ServerThread(DatagramSocket serverSocket, InetSocketAddress client, RTP rtpService) {
        this.serverSocket = serverSocket;
        clientAddr = client;
        this.rtpService = rtpService;
    }

    public void run() {
        String requestMsg = setUpConnection();
        if (requestMsg == null) return;
        String teardownMsg = respond(requestMsg);
        teardown(teardownMsg);
    }

    private String setUpConnection() {
        String msg = rtpService.getInbox().get(clientAddr).remove(0);
        if (!RTP.isSYN(msg) || RTP.isACK(msg)) {
            rtpService.getInbox().remove(clientAddr);
            return null;
        }

        DatagramPacket sendPacket = rtpService.makeConnectionPacket(msg, clientAddr, new byte[0]);
        long start = System.currentTimeMillis();
        boolean firstPacket = true;
        // TODO: check seq and ack
        while (rtpService.getInbox().get(clientAddr).isEmpty()
            || !RTP.isInOrder(sendPacket, rtpService.getInbox().get(clientAddr).get(0))
            || !RTP.isSYN(rtpService.getInbox().get(clientAddr).get(0))) {
            if (!rtpService.getInbox().get(clientAddr).isEmpty()) {
            	rtpService.getInbox().get(clientAddr).remove(0);
            }
            if (firstPacket || System.currentTimeMillis() - start >= TIMEOUT) {
                while (true) {
                    try {
                        rtpService.setRWND(sendPacket, clientAddr);
                        serverSocket.send(sendPacket);
                        firstPacket = false;
                        break;
                    } catch (IOException e) {
                        continue;
                    }
                }
                if (System.currentTimeMillis() - start >= TIMEOUT) {
                    start = System.currentTimeMillis();
                }
            }
        }
        String request = rtpService.getInbox().get(clientAddr).remove(0);
        System.out.printf("Connected with %s\n", clientAddr.toString());
        return request;
    }

    private String respond(String clientMsg) {
        String request = new String(RTP.getData(clientMsg));
        byte[] info = rtpService.getDB().query(request);

        List<DatagramPacket> sendPacket = rtpService.makeListOfPackets(clientMsg, clientAddr, info);

        int wndBase = 0; // Starting index of the sender window on the list of packets
        int wndLen = RTP.getRWND(RTP.INITIAL_WND, clientMsg);

        String msg = null;
        while (wndBase < sendPacket.size()) {
            int nextPacket = wndBase;
            for (int i = 0; i < wndLen; i++) {
                while (true) {
                    try {
                        DatagramPacket packet = sendPacket.get(wndBase);
                        rtpService.setRWND(packet, clientAddr);
                        serverSocket.send(packet);
                        nextPacket++;
                        break;
                    } catch (IOException e) {
                        continue;
                    }
                }
                if (nextPacket == sendPacket.size()) {
                    break;
                }
            }

            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < TIMEOUT) {
                if (!rtpService.getInbox().get(clientAddr).isEmpty()) {
                    msg = rtpService.getInbox().get(clientAddr).remove(0);
                    if (RTP.isInOrder(sendPacket.get(wndBase), msg)) {
                        wndBase++;
                        wndLen = RTP.getRWND(wndLen + 1, msg);
                        DatagramPacket packet;
                        if (nextPacket < sendPacket.size()) {
                            packet = sendPacket.get(nextPacket);
                            RTP.setAckNum(packet, rtpService.getAckNum(msg));
                        } else {
                            packet = rtpService.makeSendPacket(clientAddr, "100", rtpService.getAckNum(msg), new byte[0]);
                        }
                        
                        while (true) {
                            try {
                                rtpService.setRWND(packet, clientAddr);
                                serverSocket.send(packet);
                                nextPacket++;
                                break;
                            } catch (IOException e) {
                                continue;
                            }
                        }
                        start = System.currentTimeMillis();
                    } else {
                        wndLen--;
                        if (wndLen < 1) {
                            wndLen = 1;
                        }
                    }
                }
            }
        }

        while (!RTP.isFIN(msg)) {
            if (!rtpService.getInbox().get(clientAddr).isEmpty()) {
                msg = rtpService.getInbox().get(clientAddr).remove(0);
            }
        }
        return msg;
    }

    private void teardown(String clientMsg) {
        DatagramPacket sendPacket = rtpService.teardownPacket(clientMsg, clientAddr);
        while (true) {
            try {
            	rtpService.getInbox().remove(clientAddr);
            	rtpService.setRWND(sendPacket, clientAddr);
                serverSocket.send(sendPacket);
                break;
            } catch (IOException e) {
                continue;
            }
        }
    }
}