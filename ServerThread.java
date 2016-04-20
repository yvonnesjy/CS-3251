import java.lang.System;
import java.net.*;
import java.net.DatagramPacket;
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
        while (true) {
            String requestMsg = setUpConnection();
            if (requestMsg == null) return;
            String teardownMsg = respond(requestMsg);
            if (new String(RTP.getData(requestMsg)).equals("disconnect")) {
                teardown(teardownMsg);
                break;
            }
        }
    }

    private String setUpConnection() {
        while (rtpService.getInbox().get(clientAddr).isEmpty());
        String msg = rtpService.getInbox().get(clientAddr).remove(0);
        if (!RTP.isSYN(msg) || RTP.isACK(msg)) {
            rtpService.getInbox().remove(clientAddr);
            return null;
        }

        DatagramPacket sendPacket = rtpService.makeConnectionPacket(msg, clientAddr, new byte[0]);
        long start = System.currentTimeMillis();
        boolean firstPacket = true;
        
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
        String request = new String(RTP.getData(clientMsg)).trim();
        byte[] info = rtpService.getDB().query(request);

        List<DatagramPacket> sendPacket = rtpService.makeListOfPackets(clientMsg, clientAddr, info);

        int wndBase = 0; // Starting index of the sender window on the list of packets
        int wndLen = RTP.getRWND(RTP.INITIAL_WND, clientMsg);

        String msg = null;
        DatagramPacket packet = null;
        System.out.println("For " + info.length + " bytes, we have " + sendPacket.size() + " packets to send.");

        while (wndBase < sendPacket.size()) {
            System.out.println("wndBase:" + wndBase);
            int nextPacket = wndBase;
            for (int i = 0; i < wndLen; i++) {
                while (true) {
                    try {
                        packet = sendPacket.get(wndBase + i);
                        rtpService.setRWND(packet, clientAddr);
                        if (wndBase != 0 || i != 0){
                            rtpService.setAckNum(packet, String.format("%03d", Integer.parseInt(rtpService.getAckNum(new String(sendPacket.get(wndBase + i - 1).getData())))));
                        }
                        System.out.println("Mass send packet header: " + new String(packet.getData()).substring(0, 10));
                        if (RTP.getData(new String(packet.getData())) == null){
                            System.out.println("sending packet with null data");
                        }
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
            while (System.currentTimeMillis() - start < TIMEOUT && wndBase < sendPacket.size()) {
                if (!rtpService.getInbox().get(clientAddr).isEmpty()) {
                    msg = rtpService.getInbox().get(clientAddr).remove(0);
                    System.out.println("Received message header: " +  msg.substring(0, 10));
                    if (RTP.isInOrder(sendPacket.get(wndBase), msg)) {
                        wndBase++;
                        wndLen = RTP.getRWND(wndLen + 1, msg);
                        System.out.println("NextPacket:" + nextPacket + " sendPacketsize:" + sendPacket.size());
                        if (nextPacket < sendPacket.size()) {
                            packet = sendPacket.get(nextPacket);
                            RTP.setAckNum(packet, rtpService.getAckNum(msg));
                        } else {
                            System.out.println("Sending end of transmission packet");
                            packet = rtpService.makeSendPacket(clientAddr, "111", rtpService.getAckNum(msg), new byte[0]);
                        }
                        
                        while (true) {
                            try {
                                rtpService.setRWND(packet, clientAddr);

                                System.out.println("Shift sent packet header: " + new String(packet.getData()).substring(0, 10));
                                if (RTP.getData(new String(packet.getData())) == null){
                                    System.out.println("Sending packet with null data");
                                }
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
            if (wndBase >= sendPacket.size())
                System.out.println("Sent all packets.");
            else
                System.out.println("Timeout.");
        }
        
        long start = System.currentTimeMillis();
       /*while (!RTP.isFIN(msg)) {
        	if (System.currentTimeMillis() - start >= TIMEOUT) {
        		while (true) {
                    try {
                        rtpService.setRWND(packet, clientAddr);
                        serverSocket.send(packet);
                        break;
                    } catch (IOException e) {
                        continue;
                    }
                }
        		start = System.currentTimeMillis();
        	}
            if (!rtpService.getInbox().get(clientAddr).isEmpty()) {
                msg = rtpService.getInbox().get(clientAddr).remove(0);
            }
        } */
        return msg;
    }

    private void teardown(String clientMsg) {
        DatagramPacket sendPacket = rtpService.teardownPacket(clientMsg, clientAddr);
        while (true) {
            try {
            	rtpService.setRWND(sendPacket, clientAddr);
            	rtpService.getInbox().remove(clientAddr);
                serverSocket.send(sendPacket);
                break;
            } catch (IOException e) {
                continue;
            }
        }
    }
}