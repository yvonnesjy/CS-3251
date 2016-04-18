import java.util.*;
import java.net.*;

public class DBEngine {
    public static void main(String[] args) {
        Database db = initializeDB();

        if (args.length != 1) {
            throw new IllegalArgumentException("Invalid arguments");
        }

        int port;
        try {
            port = Integer.valueOf(args[0]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid port number");
        }

        try {
            RTP rtpService = new RTP(db);
            rtpService.serverRoutine(port);
        } catch (SocketException e) {
            System.err.println("SocketException: " + e.getMessage());
        }
    }

    private static Database initializeDB() {
        List<String> ids = Arrays.asList("903076259", "903084074", "903077650", "903083691", "903082265", "903075951", "903084336", "902987866", "000000000");
        String[] fn = {"Anthony", "Richard", "Joe", "Todd", "Laura", "Marie", "Stephen", "Yvonne", "Bud"};
        String[] ln = {"Peterson", "Harris", "Miller", "Collins", "Stewart", "Cox", "Baker", "Shi", "Peterson"};
        int[] points = {231, 236, 224, 218, 207, 246, 234, 300, 100};
        int[] hours = {63, 66, 65, 56, 64, 63, 66, 77, 100};

        return new DBDatabase(ids, fn, ln, points, hours);
    }

}