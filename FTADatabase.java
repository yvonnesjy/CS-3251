public class FTADatabase implements Database {
    public byte[] query(String request) {
        Path path = Paths.get(request);
        byte[] file = Files.readAllBytes(path);
    }
}