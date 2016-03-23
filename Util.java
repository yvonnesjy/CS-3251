public class Util {
    final static int ACK_NUM_IND = 0;
    final static int ACK_NUM_OFFSET = 1;
    final static int SEQ_NUM_IND = 1;
    final static int SEQ_NUM_OFFSET = 1;
    final static int ACK = 2;
    final static int SYN = 3;
    final static int FIN = 4;
    final static int DATA_IND = 5;
    final static int PACKETSIZE = 1000;
    final static int MAX_SEQ_NUM = 1;

    // public byte[] packetize(String msg) {

    // }

    public static String getAckNum(String content) throws Exception {
        int seq;
        try {
            seq = Integer.valueOf(content.substring(SEQ_NUM_IND, SEQ_NUM_IND + SEQ_NUM_OFFSET));
        } catch (Exception e) {
            throw e;
        }
        if (seq + 1 > MAX_SEQ_NUM) {
            throw new Exception();
        } else {
            return String.valueOf(seq + 1);
        }
    }

    public static void printInfo(String infoRec, boolean fn, boolean ln, boolean point, boolean hours, boolean gpa) throws Exception {
        System.out.print("From server:");
        String[] info = infoRec.split(" ");
        if (fn) {
            System.out.print(" first_name: " + info[0]);
        }
        if (ln) {
            System.out.print(" last_name: " + info[1]);
        }
        if (point) {
            System.out.print(" quality_points: " + info[2]);
        }
        if (hours) {
            System.out.print(" gpa_hours: " + info[3]);
        }
        if (gpa) {
            System.out.format(" gpa: " + (float)info[2]/info[3]);
        }
    }

    public static void main(String[] args) {
        
    }
}