import java.lang.String;
import java.lang.System;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.net.*;
import java.io.*;

public class ClientThread implements Callable<byte[]> {
	final static int TIMEOUT = 1000;
	
    private DatagramSocket clientSocket;
    private InetSocketAddress serverAddr;
    private String request;
    private RTP rtpService;

    public ClientThread(DatagramSocket clientSocket, InetSocketAddress serverAddr, String request, RTP rtpService) {
        this.clientSocket = clientSocket;
        this.serverAddr = serverAddr;
        this.request = request;
        this.rtpService = rtpService;
    }
    
    public byte[] call() {
        String connectionConfirmMsg = setUpConnection(clientSocket, serverAddr);
        byte[] buffer = null;
        ArrayList<Object> catchOutput = requestInfo(connectionConfirmMsg, clientSocket, serverAddr, request);
        String infoMsg = (String)catchOutput.get(0);
        buffer = (byte[])catchOutput.get(1);
        if (request.equals("disconnect"))
            teardown(infoMsg, clientSocket, serverAddr);
        return buffer;
    }
    
    private String setUpConnection(DatagramSocket clientSocket, InetSocketAddress serverAddr) {
        DatagramPacket sendPacket = rtpService.makeConnectionPacket(null, serverAddr, new byte[0]);
        
        String msg = null;
        boolean pass = false;
        while (!pass) {
            try {
                clientSocket.send(sendPacket);
            } catch (IOException e) {
                continue;
            }
            
            long start = System.currentTimeMillis();
            while (!pass && System.currentTimeMillis() - start < TIMEOUT) {
                if (!rtpService.getInbox().get(serverAddr).isEmpty()) {
                    msg = rtpService.getInbox().get(serverAddr).remove(0);
                    if (RTP.isInOrder(sendPacket, msg)
                        && RTP.isSYN(msg)) {
                        pass = true;
                    }
                }
            }
        }

        return msg;
    }

    private ArrayList<Object> requestInfo(String serverMsg, DatagramSocket clientSocket, InetSocketAddress serverAddr, String request) {
        DatagramPacket sendPacket = rtpService.makeConnectionPacket(serverMsg, serverAddr, request.getBytes());
        
        String msg = null;
        boolean pass = false;
        byte[] buffer;
        ArrayList<Object> output = new ArrayList<Object>();
        while(!pass) {
            try {
            	rtpService.setRWND(sendPacket, serverAddr);
                System.out.println("Request packet sent: " +  new String(sendPacket.getData()).substring(0, 10));
                clientSocket.send(sendPacket);
            } catch (IOException e) {
                continue;
            }

            long start = System.currentTimeMillis();
            while (!pass && System.currentTimeMillis() - start < TIMEOUT) {
                if (!rtpService.getInbox().get(serverAddr).isEmpty()) {
                    msg = rtpService.getInbox().get(serverAddr).remove(0);
                    System.out.println("Received packet: " + msg.substring(0, 10));
                    if (RTP.isInOrder(sendPacket, msg)) {
                        pass = true;
                    }
                }
            }
            System.out.println("Timeout.");
        }
        
        buffer = RTP.getData(msg);
        DatagramPacket latestUnacked = rtpService.makeSendPacket(serverAddr, "100", rtpService.getAckNum(msg), new byte[0]);
        while (true) {
            try {
            	rtpService.setRWND(latestUnacked, serverAddr);

                clientSocket.send(latestUnacked);
                break;
            } catch (IOException e) {
                continue;
            }
        }
        boolean allAcked = false;
        boolean STOP = false;

        while (!allAcked) {
            if (!rtpService.getInbox().get(serverAddr).isEmpty()) {
                System.out.println("Lets process a packet.");
                msg = rtpService.getInbox().get(serverAddr).remove(0);
                if (RTP.isInOrder(latestUnacked, msg) && !msg.substring(0,3).equals("111")) {
                    System.out.println("Packet in order, has data");
                	buffer = putInBuffer(buffer, msg);
                	System.out.println("we made it past buffer");
                    latestUnacked = rtpService.makeSendPacket(serverAddr, "100", rtpService.getAckNum(msg), new byte[0]);
                    System.out.println("we made it past latestUnacked");
                    
                    while (true) {
                        try {
                        	rtpService.setRWND(latestUnacked, serverAddr);
                            System.out.println("Sending ACK with Header: " + new String(latestUnacked.getData()).substring(0,10));
                            clientSocket.send(latestUnacked);
                            break;
                        } catch (IOException e) {
                            System.out.println("Exception when sending ACK with header: " + new String(latestUnacked.getData()).substring(0,1));
                            continue;
                        }
                    }
                } else if (RTP.isInOrder(latestUnacked, msg)) {
                    if (msg.substring(0,3).equals("111"));
                        System.out.println("End of transmission");
                	allAcked = true;
                } else {
                    System.out.println("Out of order packet.");
                	while (true) {
                        try {
                        	rtpService.setRWND(latestUnacked, serverAddr);
                            clientSocket.send(latestUnacked);
                            break;
                        } catch (IOException e) {
                            continue;
                        }
                    }
                }
                STOP = false;
            } else {
                if (!STOP) {
                    System.out.println("We empty BoyZ");
                    STOP = true;
                }
            }
        }
        output.add(msg);
        output.add(buffer);

        return output;
    }
    
    private byte[] putInBuffer(byte[] buffer, String msg) {
    	return RTP.merge(buffer, RTP.getData(msg));
    }

    private void teardown(String serverMsg, DatagramSocket clientSocket, InetSocketAddress serverAddr) {
        DatagramPacket sendPacket = rtpService.teardownPacket(serverMsg, serverAddr);
        while (true) {
            try {
            	rtpService.setRWND(sendPacket, serverAddr);
                clientSocket.send(sendPacket);
                break;
            } catch (IOException e) {
                continue;
            }
        }
    }
}