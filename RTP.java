import java.lang.Byte;
import java.lang.System;
import java.net.*;
import java.lang.IndexOutOfBoundsException;
import java.net.DatagramSocket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;

public class RTP {
    // |ACK|SYN|FIN|SEQ_NUM|ACK_NUM|CHECKSUM|RWND|DATA|
    //  0   1   2   3     5 6     8 9        10   11  1000
    final static int ACK = 0;
    final static int SYN = 1;
    final static int FIN = 2;
    final static int SEQ_IND = 3;
    final static int SEQ_LEN = 3;
    final static int MAX_SEQ_NUM = (int) Math.pow(10, SEQ_LEN);
    final static int ACK_IND = SEQ_IND + SEQ_LEN;
    final static int ACK_LEN = 3;
    final static int MAX_ACK_NUM = (int) Math.pow(10, ACK_LEN);
    final static int CHECKSUM = 9;
    final static int RWND_IND = 10;
    final static int RWND_LEN = 1;
    final static int DATA_IND = 11;
    final static int HEADER_SIZE = 11;
    final static int MAX_PACKET_SIZE = 1000;
    final static int INITIAL_WND = 5;

    private int ackNum;
    private int seqNum;
    private int maxWnd;

    private Hashtable<InetSocketAddress, List<String>> inbox;
    private Database db;

    public RTP(int maxWnd) {
        seqNum = 0;
        inbox = new Hashtable<>();
        this.maxWnd = maxWnd;
    }

    public RTP(Database db) {
        seqNum = 0;
        inbox = new Hashtable<>();
        this.db = db;
    }
    
    public Hashtable<InetSocketAddress, List<String>> getInbox() {
    	return inbox;
    }
    
    public Database getDB() {
    	return db;
    }

    public static boolean isACK(String packet) throws IndexOutOfBoundsException {
        try {
            return packet.charAt(ACK) == '1';
        } catch (IndexOutOfBoundsException e) {
            throw e;
        }
    }

    public static boolean isSYN(String packet) throws IndexOutOfBoundsException {
        try {
            return packet.charAt(SYN) == '1';
        } catch (IndexOutOfBoundsException e) {
            throw e;
        }
    }

    public static boolean isFIN(String packet) throws IndexOutOfBoundsException {
    	if (packet == null) {
    		return false;
    	}
        try {
            return packet.charAt(FIN) == '1';
        } catch (IndexOutOfBoundsException e) {
            throw e;
        }
    }

    public static boolean checkSum(byte[] packet) {
    	if (packet.length <= DATA_IND) {
    		return true;
    	}
        byte checksum = packet[DATA_IND];
        for (int i = DATA_IND + 1; i < packet.length; i++) {
            if (i != CHECKSUM) {
                checksum ^= packet[i];
            }
        }
        
        return true;
        //return checksum == packet[CHECKSUM];
    }

    public static boolean isInOrder(DatagramPacket sentPacket, String ackMsg) throws IndexOutOfBoundsException {
        if (!isACK(ackMsg)) {
            return false;
        }
        String sentMsg = new String(sentPacket.getData());
        String seq = responseAckNum(sentMsg);
        try {
            return ackMsg.substring(ACK_IND, ACK_IND + ACK_LEN).equals(seq); // ???????
        } catch (IndexOutOfBoundsException e) {
            throw e;
        }
    }

    public DatagramPacket makeConnectionPacket(String inputMsg, InetSocketAddress clientAddr, byte[] data) {
        return makeSendPacket(clientAddr, inputMsg == null ? "010" : "110", inputMsg == null ? getAckNum(null) : initializeAckNum(inputMsg), data);
    }

    public List<DatagramPacket> makeListOfPackets(String inputMsg, InetSocketAddress clientAddr, byte[] data) {
        List<DatagramPacket> packetList = new LinkedList<>();
        int maxContentSize = MAX_PACKET_SIZE - HEADER_SIZE;
        int numIterations = (int) Math.ceil((double) data.length / maxContentSize);
        for (int i = 0; i < numIterations; i++) {
            byte[] subContent = Arrays.copyOfRange(data, i * maxContentSize, (i + 1) * maxContentSize);
            DatagramPacket sendPacket = makeSendPacket(clientAddr, "100", i == 0 ? getAckNum(inputMsg) : String.format("%0" + ACK_LEN + "d", 0), subContent);
            packetList.add(sendPacket);
        }
        return packetList;
    }

    public DatagramPacket teardownPacket(String inputMsg, InetSocketAddress clientAddr) {
        return makeSendPacket(clientAddr, "101", getAckNum(inputMsg), new byte[0]);
    }

    public DatagramPacket makeSendPacket(InetSocketAddress clientAddr, String flags, String ack_num, byte[] data) {
        byte[] sendData = merge((flags + getSeqNum() + ack_num).getBytes(), new byte[1], new byte[RWND_LEN], data);
        if (DATA_IND >= sendData.length) {
        	sendData[CHECKSUM] = 0;
        } else {
	        byte checksum = sendData[DATA_IND];
	        for (int i = DATA_IND + 1; i < sendData.length; i++) {
	            checksum ^= sendData[i];
	        }
	        sendData[CHECKSUM] = checksum;
        }
        return new DatagramPacket(sendData, sendData.length, clientAddr.getAddress(), clientAddr.getPort());
    }

    public static byte[] merge(byte[] ...arrays ) {
        int size = 0;
        for (byte[] a: arrays)
            size += a.length;

        byte[] res = new byte[size];

        int destPos = 0;
        for (int i = 0; i < arrays.length; i++) {
            if (i > 0) {
                destPos += arrays[i-1].length;
            }
            int length = arrays[i].length;
            System.arraycopy(arrays[i], 0, res, destPos, length);
        }

        return res;
    }

    public static DatagramPacket makeReceivePacket() {
        byte[] receiveData = new byte[MAX_PACKET_SIZE];
        return new DatagramPacket(receiveData, receiveData.length);
    }

    public void setRWND(DatagramPacket packet, InetSocketAddress addr) {
    	String msg = new String(packet.getData());
    	String rwnd = maxWnd - inbox.get(addr).size() > 0 ? maxWnd - inbox.get(addr).size() + "" : String.format("%0" + RWND_LEN + "d", 1);
    	byte[] data = (msg.substring(0, RWND_IND) + rwnd + msg.substring(RWND_IND + RWND_LEN)).getBytes();
    	packet.setData(data);
    }
    
    public String getAckNum(String msg) {
        if (msg == null) {
            return String.format("%0" + ACK_LEN + "d", 0);
        }
        int num = Integer.valueOf(msg.substring(SEQ_IND, SEQ_IND + SEQ_LEN));
        if (num == ackNum) {
            ackNum++;
        }
        if (ackNum == MAX_ACK_NUM) {
            ackNum = 0;
        }
        return String.format("%0" + ACK_LEN + "d", ackNum);
    }
    
    public static void setAckNum(DatagramPacket packet, String ackNum) {
    	String msg = new String(packet.getData());
    	byte[] data = (msg.substring(0, ACK_IND) + ackNum + msg.substring(ACK_IND + ACK_LEN)).getBytes();
    	packet.setData(data);
    }

    public String getSeqNum() {
    	String num = String.format("%0" + SEQ_LEN + "d", seqNum);
    	seqNum++;
    	if (seqNum == MAX_SEQ_NUM) {
    		seqNum = 0;
    	}
        return num;
    }

    public static int getRWND(int cur, String msg) {
        int rwnd = Integer.valueOf(msg.substring(RWND_IND, RWND_IND + RWND_LEN));
        return Math.min(cur, rwnd);
    }

    public static byte[] getData(String msg) {
    	/*if (DATA_IND >= msg.trim().length()) {
    		return null;
    	}*/
        return msg.substring(DATA_IND).getBytes();
    }
    
    public static String responseAckNum(String msg) {
    	int num = Integer.valueOf(msg.substring(SEQ_IND, SEQ_IND + SEQ_LEN)) + 1;
    	if (num == MAX_SEQ_NUM) {
    		num = 0;
    	}
    	return String.format("%0" + SEQ_LEN + "d", num);
    }

    public String initializeAckNum(String msg) {
        ackNum = Integer.valueOf(msg.substring(SEQ_IND, SEQ_IND + SEQ_LEN)) + 1;
        return String.format("%0" + ACK_LEN + "d", ackNum);
    }




    public void serverRoutine(int port) throws SocketException {
        DatagramSocket serverSocket = new DatagramSocket(port);
        System.out.println("The server is ready to receive");

        while (true) {
            DatagramPacket receivePacket = makeReceivePacket();
            try {
                serverSocket.receive(receivePacket);
            } catch (IOException e) {
                continue;
            }
            if (!checkSum(receivePacket.getData())) {
                continue;
            }
            InetSocketAddress client = Multiplexer.multiplex(receivePacket, inbox);

            if (client != null) {
                new Thread(new ServerThread(serverSocket, client, this)).start();
            }
        }
    }
    
    public byte[] clientRoutine(DatagramSocket clientSocket, InetAddress ip, int port, String request) throws SocketException {
        InetSocketAddress serverAddr = new InetSocketAddress(ip, port);
        List<String> packetQueue = new LinkedList<>();
        inbox.put(serverAddr, packetQueue);
        
        ExecutorService pool = Executors.newFixedThreadPool(1);
        ClientThread callable = new ClientThread(clientSocket, serverAddr, request, this); 
        Future<byte[]> future = pool.submit(callable);

        while (!future.isDone()) {
            DatagramPacket receivePacket = makeReceivePacket();
            try {
                clientSocket.receive(receivePacket);
                System.out.println("Received DatagramPacket with RTP header: " + new String(receivePacket.getData()).substring(0, 10));
            } catch (IOException e) {
                System.out.println("Exception on receiving datagram.");
                continue;
            }
            if (!checkSum(receivePacket.getData())) {
                System.out.println("Exception on checksum.");
                continue;
            }
            
            InetSocketAddress addr;
            try {
                addr = (InetSocketAddress) receivePacket.getSocketAddress();
            } catch (IllegalArgumentException e) { // To filter some system internet configuration socket
                System.out.println("Exception getting socket address from datagram.");
                continue;
            }
            
            if (!addr.equals(serverAddr)) {
                System.out.println("Exception matching packet addr to server addr.");
                continue;
            }
            byte[] rtpPacket = receivePacket.getData();
            String rtpData = new String(receivePacket.getData());
            System.out.println("About to try to put Datagram into Queue.");
            if (packetQueue.isEmpty() || !packetQueue.get(packetQueue.size() - 1).equals(rtpData)) {
                System.out.println("Adding packet to Queue with header: " + rtpData.substring(0,10) + " and " + rtpPacket.length + " total bytes");
                packetQueue.add(rtpData);
            } else {
                System.out.println("Queue empty or duplicate packet detected.");
            }
        }
        try {
        	return future.get();
        } catch (Exception e) {
        	return null;
        }
    }
}