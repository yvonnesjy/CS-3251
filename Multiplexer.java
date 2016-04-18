import java.net.*;
import java.util.*;

public class Multiplexer {
    public static InetSocketAddress multiplex(DatagramPacket receivePacket, Hashtable<InetSocketAddress, List<String>> inbox) {
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