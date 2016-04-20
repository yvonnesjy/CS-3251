import java.lang.System;
import java.nio.file.*;
import java.io.*;

public class FTADatabase implements Database {
    public byte[] query(String request) {
        Path path = Paths.get(request);
        byte[] file;
        try {
            file = Files.readAllBytes(path);
        } catch (Exception e) {
            file = "File not Found".getBytes();
        }
        return file;
    }
}