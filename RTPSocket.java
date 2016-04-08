public class RTPSocket {
    private DatagramSocket socket;
    private InetAddress ip;
    private int port;
    
    //Constructor for RTP server
    public RTPSocket(int port) {
        socket = new DatagramSocket(port);
    }

    //Constructor for RTP client
    public RTPSocket(InetAddress ip, int port) {
        socket = new DatagramSocket();
        this.ip = ip;
        this.port = port;
        
        //Client side three way handshake
        
    }
    
    //Server side three way handshake
    public void accept() {
        
    }
    
    public writeBytes(byte[] bytes) {
        
    }
    
    public byte[] readBytes() {
    
    }
    
    private send() {
    }
    
    private receive() {
    }
}
