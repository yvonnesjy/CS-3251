import java.util.*;

public class DBDatabase implements Database {
    private List<String> ids;
    private String[] fn;
    private String[] ln;
    private int[] points;
    private int[] hours;

    public DBDatabase(List<String> ids, String[] fn, String[] ln, int[] points, int[] hours) {
        this.ids = ids;
        this.fn = fn;
        this.ln = ln;
        this.points = points;
        this.hours = hours;
    }

    public byte[] query(String request) {
        int ind = ids.indexOf(request.trim());
        String info;
        if (ind == -1) {
            info = "Invalid GTID";
        } else {
            info = fn[ind] + " " + ln[ind] + " " + points[ind] + " " + hours[ind];
        }
        return info.getBytes();
    }
}