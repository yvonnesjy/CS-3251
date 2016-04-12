import java.net.*;
import java.lang.IndexOutOfBoundsException;
import java.util.*;

public class RTP {
    // |ACK|SYN|FIN|SEQ_NUM|ACK_NUM|DATA|
    //  0   1   2   3     5 6     8 9  1000
    final static int ACK = 0;
    final static int SYN = 1;
    final static int FIN = 2;
    final static int SEQ_IND = 3;
    final static int SEQ_LEN = 3;
    final static int MAX_SEQ_NUM = (int) Math.pow(10, SEQ_LEN);
    final static int ACK_IND = SEQ_IND + SEQ_LEN;
    final static int ACK_LEN = 3;
    final static int MAX_ACK_NUM = (int) Math.pow(10, ACK_LEN);
    final static int DATA_IND = ACK_IND + ACK_LEN;
    final static int HEADER_SIZE = 3 + SEQ_LEN + ACK_LEN;
    final static int MAX_PACKET_SIZE = 1000;

    private int ackNum;
    private int seqNum;

    public RTP() {
        seqNum = 0;
    }

    public boolean isACK(String packet) throws IndexOutOfBoundsException {
        try {
            return packet.charAt(ACK) == '1';
        } catch (IndexOutOfBoundsException e) {
            throw e;
        }
    }

    public boolean isSYN(String packet) throws IndexOutOfBoundsException {
        try {
            return packet.charAt(SYN) == '1';
        } catch (IndexOutOfBoundsException e) {
            throw e;
        }
    }

    public boolean isFIN(String packet) throws IndexOutOfBoundsException {
        try {
            return packet.charAt(FIN) == '1';
        } catch (IndexOutOfBoundsException e) {
            throw e;
        }
    }

    public boolean isInOrder(DatagramPacket sentPacket, String ackMsg) throws IndexOutOfBoundsException {
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

    public DatagramPacket makeConnectionPacket(String inputMsg, InetSocketAddress clientAddr, String data) {
        String outputMsg = (inputMsg == null ? "010" : "110")
            + getSeqNum()
            + (inputMsg == null ? getAckNum(null) : initializeAckNum(inputMsg))
            + data;
        byte[] sendData = outputMsg.getBytes();
        return new DatagramPacket(sendData, sendData.length, clientAddr.getAddress(), clientAddr.getPort());
    }

    public List<DatagramPacket> makePackets(String inputMsg, InetSocketAddress clientAddr, String data) {
        List<DatagramPacket> packetList = new LinkedList<>();
        int maxContentSize = MAX_PACKET_SIZE - HEADER_SIZE;
        int numIterations = (int) Math.ceil((double) data.length() / maxContentSize);
        for (int i = 0; i < numIterations; i++) {
            String subContent = i == numIterations - 1 ? data.substring(i * maxContentSize) : data.substring(i * maxContentSize, (i + 1) * maxContentSize);

            String outputMsg = "100" + getSeqNum() + getAckNum(inputMsg) + subContent;

            byte[] sendData = outputMsg.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddr.getAddress(), clientAddr.getPort());
            packetList.add(sendPacket);
        }
        return packetList;
    }

    public DatagramPacket teardownPacket(String inputMsg, InetSocketAddress clientAddr) {
        String outputMsg = "101" + getSeqNum() + getAckNum(inputMsg);
        byte[] sendData = outputMsg.getBytes();
        return new DatagramPacket(sendData, sendData.length, clientAddr.getAddress(), clientAddr.getPort());
    }

    public static DatagramPacket makeReceivePacket() {
        byte[] receiveData = new byte[MAX_PACKET_SIZE];
        return new DatagramPacket(receiveData, receiveData.length);
    }

    public String getAckNum(String msg) {
        if (msg == null) {
            return String.format("%0" + ACK_LEN + "d", 0);
        }
        int num = Integer.valueOf(msg.substring(SEQ_IND, SEQ_IND + SEQ_LEN));
        if (num == ackNum) {
            ackNum++;
        }
        return String.format("%0" + ACK_LEN + "d", ackNum);
    }

    public String getSeqNum() {
    	String num = String.format("%0" + SEQ_LEN + "d", seqNum);
    	seqNum++;
    	if (seqNum == MAX_SEQ_NUM) {
    		seqNum = 0;
    	}
        return num;
    }

    public String getData(String msg) throws IndexOutOfBoundsException {
        try {
            return msg.substring(DATA_IND);
        } catch (IndexOutOfBoundsException e) {
            throw e;
        }
    }
    
    public String responseAckNum(String msg) {
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
}