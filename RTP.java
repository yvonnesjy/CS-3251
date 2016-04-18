import java.net.*;
import java.lang.IndexOutOfBoundsException;
import java.util.*;
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
    // final static int RWND_IND = 10;
    // final static int RWND_LEN = 1;
    final static int DATA_IND = 10;
    final static int HEADER_SIZE = 10;
    final static int MAX_PACKET_SIZE = 1000;
    final static int TIMEOUT = 1000;
    final static int INITIAL_WND = 5;

    private int ackNum;
    private int seqNum;
    private int maxWnd;

    private Hashtable<InetSocketAddress, List<String>> inbox;
    private Database db;

    public RTP(int maxWnd) {
        seqNum = 0;
        this.maxWnd = maxWnd;
    }

    public RTP(Database db) {
        seqNum = 0;
        inbox = new Hashtable<>();
        this.db = db;
    }

    private boolean isACK(String packet) throws IndexOutOfBoundsException {
        try {
            return packet.charAt(ACK) == '1';
        } catch (IndexOutOfBoundsException e) {
            throw e;
        }
    }

    private boolean isSYN(String packet) throws IndexOutOfBoundsException {
        try {
            return packet.charAt(SYN) == '1';
        } catch (IndexOutOfBoundsException e) {
            throw e;
        }
    }

    private boolean isFIN(String packet) throws IndexOutOfBoundsException {
        try {
            return packet.charAt(FIN) == '1';
        } catch (IndexOutOfBoundsException e) {
            throw e;
        }
    }

    private boolean checkSum(byte[] packet) {
        byte checksum = packet[0];
        for (int i = 1; i < packet.length; i++) {
            if (i != CHECKSUM) {
                checksum ^= packet[i];
            }
        }
        return checksum == packet[CHECKSUM];
    }

    private boolean isInOrder(int base, String ackMsg) {
        if (!isACK(ackMsg)) {
            return false;
        }
        return Integer.valueOf(ackMsg.substring(ACK_IND, ACK_IND + ACK_LEN)).equals(base + 1);
    }

    private boolean isInOrder(DatagramPacket sentPacket, String ackMsg) throws IndexOutOfBoundsException {
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

    private DatagramPacket makeConnectionPacket(String inputMsg, InetSocketAddress clientAddr, byte[] data) {
        return makeSendPacket(clientAddr, inputMsg == null ? "010" : "110", inputMsg == null ? getAckNum(null) : initializeAckNum(inputMsg), data);
    }

    private List<DatagramPacket> makeListOfPackets(String inputMsg, InetSocketAddress clientAddr, byte[] data) {
        List<DatagramPacket> packetList = new LinkedList<>();
        int maxContentSize = MAX_PACKET_SIZE - HEADER_SIZE;
        int numIterations = (int) Math.ceil((double) data.length / maxContentSize);
        for (int i = 0; i < numIterations; i++) {
            byte[] subContent = Arrays.copyOfRange(data, i * maxContentSize, (i + 1) * maxContentSize);
            DatagramPacket sendPacket = makeSendPacket(clientAddr, "100", getAckNum(inputMsg), subContent);
            packetList.add(sendPacket);
        }
        return packetList;
    }

    private DatagramPacket teardownPacket(String inputMsg, InetSocketAddress clientAddr) {
        return makeSendPacket(clientAddr, "101", getAckNum(inputMsg), new byte[0]);
    }

    private DatagramPacket makeSendPacket(InetSocketAddress clientAddr, String flags, String ack_num, byte[] data) {
        byte[] sendData = merge((flags+getSeqNum()+ack_num).getBytes(), new byte[1], data);
        byte checksum = sendData[0];
        for (int i = 1; i < sendData.length; i++) {
            if (i != CHECKSUM) {
                checksum ^= sendData[i];
            }
        }
        sendData[CHECKSUM] = checksum;
        return new DatagramPacket(sendData, sendData.length, clientAddr.getAddress(), clientAddr.getPort());
    }

    private byte[] merge(byte[] ...arrays ) {
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

    private DatagramPacket makeReceivePacket() {
        byte[] receiveData = new byte[MAX_PACKET_SIZE];
        return new DatagramPacket(receiveData, receiveData.length);
    }

    private String getAckNum(String msg) {
        if (msg == null) {
            return String.format("%0" + ACK_LEN + "d", 0);
        }
        int num = Integer.valueOf(msg.substring(SEQ_IND, SEQ_IND + SEQ_LEN));
        if (num == ackNum) {
            ackNum++;
        }
        return String.format("%0" + ACK_LEN + "d", ackNum);
    }

    private String getSeqNum() {
    	String num = String.format("%0" + SEQ_LEN + "d", seqNum);
    	seqNum++;
    	if (seqNum == MAX_SEQ_NUM) {
    		seqNum = 0;
    	}
        return num;
    }

    // private int getRWND(int cur, String msg) {
    //     int rwnd = Integer.valueOf(msg.substring(RWND_IND, RWND_IND + RWND_LEN));
    //     return Math.min(cur, rwnd);
    // }

    private byte[] getData(String msg) throws IndexOutOfBoundsException {
        try {
            return msg.substring(DATA_IND).getBytes();
        } catch (IndexOutOfBoundsException e) {
            throw e;
        }
    }
    
    private String responseAckNum(String msg) {
    	int num = Integer.valueOf(msg.substring(SEQ_IND, SEQ_IND + SEQ_LEN)) + 1;
    	if (num == MAX_SEQ_NUM) {
    		num = 0;
    	}
    	return String.format("%0" + SEQ_LEN + "d", num);
    }

    private String initializeAckNum(String msg) {
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
                new Thread(new connectionThread(serverSocket, client, this)).start();
            }
        }
    }

    private class connectionThread implements Runnable {
        private DatagramSocket serverSocket;
        private InetSocketAddress clientAddr;
        private RTP rtpService;

        public connectionThread(DatagramSocket serverSocket, InetSocketAddress client, RTP rtpService) {
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
            String msg = rtpService.inbox.get(clientAddr).remove(0);
            if (!rtpService.isSYN(msg) || rtpService.isACK(msg)) {
                rtpService.inbox.remove(clientAddr);
                return null;
            }

            DatagramPacket sendPacket = rtpService.makeConnectionPacket(msg, clientAddr, new byte[0]);
            int count = 0;
            boolean firstPacket = true;
            // TODO: check seq and ack
            while (rtpService.inbox.get(clientAddr).isEmpty()
                || !rtpService.isInOrder(sendPacket, rtpService.inbox.get(clientAddr).get(0))
                || !rtpService.isSYN(rtpService.inbox.get(clientAddr).get(0))) {
                count++;
                if (!rtpService.inbox.get(clientAddr).isEmpty()) {
                    rtpService.inbox.get(clientAddr).remove(0);
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
            String request = rtpService.inbox.get(clientAddr).remove(0);
            System.out.printf("Connected with %s\n", clientAddr.toString());
            return request;
        }

        private String respond(String clientMsg) {
            String request = new String(rtpService.getData(clientMsg));
            byte[] info = db.query(request);

            // Congestion control stuff

            // List<DatagramPacket> sendPacket = rtpService.makeListOfPackets(clientMsg, clientAddr, info);
            // int base = Integer.valueOf(new String(sendPacket.get(0).getData()).substring(SEQ_IND, SEQ_IND + SEQ_LEN));

            // int wndBase = 0;
            // int wndLen = getRWND(INITIAL_WND, clientMsg);

            // String msg;
            // while (base < sendPacket.size()) {

            //     for (int i = 0; i < wnd; i++) {
            //         while (true) {
            //             try {
            //                 serverSocket.send(sendPacket.get(base + i));
            //                 break;
            //             } catch (IOException e) {
            //                 continue;
            //             }
            //         }
            //     }

            //     Thread timer = new Thread(new Runnable() {
            //         public void run() {
            //             sleep(TIMEOUT);
            //             thisThread.interrupt();
            //         }
            //     });
            //     timer.start();

            //     int count = 0;
            //     while (!Thread.interrupted()) {
            //         count++;
            //         if (!rtpService.inbox.get(clientAddr).isEmpty()) {
            //             msg = rtpService.inbox.get(clientAddr).remove(0);
            //             if (rtpService.isInOrder(base, msg)) {
            //                 base++;
            //                 if (base == MAX_SEQ_NUM) {
            //                     base = 0;
            //                 }

            //             }
            //         }


            //         rtpService.inbox.get(clientAddr).isEmpty()
            //         || !rtpService.isInOrder(sendPacket, rtpService.inbox.get(clientAddr).get(0))
            //         || !rtpService.isFIN(rtpService.inbox.get(clientAddr).get(0))
            //     }
            // }

            int count = 0;
            boolean firstPacket = true;

            DatagramPacket sendPacket = rtpService.makeListOfPackets(clientMsg, clientAddr, info).get(0);

            while (rtpService.inbox.get(clientAddr).isEmpty()
                || !rtpService.isInOrder(sendPacket, rtpService.inbox.get(clientAddr).get(0))
                || !rtpService.isFIN(rtpService.inbox.get(clientAddr).get(0))) {
                count++;
                if (!rtpService.inbox.get(clientAddr).isEmpty()) {
                    rtpService.inbox.get(clientAddr).remove(0);
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

            String teardown = rtpService.inbox.get(clientAddr).remove(0);
            return teardown;
        }

        private void teardown(String clientMsg) {
            DatagramPacket sendPacket = rtpService.teardownPacket(clientMsg, clientAddr);
            while (true) {
                try {
                    rtpService.inbox.remove(clientAddr);
                    serverSocket.send(sendPacket);
                    break;
                } catch (IOException e) {
                    continue;
                }
            }
        }
    }

    public byte[] clientRoutine(InetAddress ip, int port, String id) throws SocketException {
        DatagramSocket clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(TIMEOUT);
        InetSocketAddress serverAddr = new InetSocketAddress(ip, port);

        String connectionConfirmMsg = setUpConnection(clientSocket, serverAddr);
        String infoMsg = requestInfo(connectionConfirmMsg, clientSocket, serverAddr, id);
        teardown(infoMsg, clientSocket, serverAddr);
        return getData(infoMsg);
    }

    private String setUpConnection(DatagramSocket clientSocket, InetSocketAddress serverAddr) {
        DatagramPacket sendPacket = makeConnectionPacket(null, serverAddr, new byte[0]);
        DatagramPacket receivePacket = makeReceivePacket();

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
                    || !checkSum(receivePacket.getData())
                    || !isInOrder(sendPacket, msg)
                    || !isSYN(msg)) {
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

    private String requestInfo(String serverMsg, DatagramSocket clientSocket, InetSocketAddress serverAddr, String id) {
        DatagramPacket sendPacket = makeConnectionPacket(serverMsg, serverAddr, id.getBytes());
        DatagramPacket receivePacket = makeReceivePacket();

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
                    || !checkSum(receivePacket.getData())
                    || !isInOrder(sendPacket, msg)) {
                    continue;
                }
                pass = true;
                break;
            }

            if (pass) {
                return new String(receivePacket.getData());
            }
        }
    }

    private void teardown(String serverMsg, DatagramSocket clientSocket, InetSocketAddress serverAddr) {
        DatagramPacket sendPacket = teardownPacket(serverMsg, serverAddr);
        while (true) {
            try {
                clientSocket.send(sendPacket);
                break;
            } catch (IOException e) {
                continue;
            }
        }
    }
}