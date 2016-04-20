import java.util.*;
import java.net.*;

public class FTAEngine {
    final static int TIMEOUT = 1000;
    static Hashtable<InetSocketAddress, List<String>> inbox;

    public static void main(String[] args) {
    	Database db = new FTADatabase();

        if (args.length != 2) {
            throw new IllegalArgumentException("Invalid arguments");
        }

        int port;
        try {
            port = Integer.valueOf(args[0]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid port number");
        }
        
        int sizeW;
        try {
        	sizeW = Integer.valueOf(args[1]);
        } catch (Exception e) {
        	throw new IllegalArgumentException("Invalid window size");
        }

        try {
            RTP rtpService = new RTP(db);
            rtpService.serverRoutine(port);
        } catch (SocketException e) {
            System.err.println("SocketException: " + e.getMessage());
        }
    }
}