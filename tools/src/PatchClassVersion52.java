import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PatchClassVersion52 {
    public static void main(String[] args) throws Exception {
        Path in = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        byte[] bytes = Files.readAllBytes(in);
        if (bytes.length < 8
                || (bytes[0] & 0xff) != 0xca
                || (bytes[1] & 0xff) != 0xfe
                || (bytes[2] & 0xff) != 0xba
                || (bytes[3] & 0xff) != 0xbe) {
            throw new IllegalArgumentException("Not a Java class file: " + in);
        }
        bytes[6] = 0;
        bytes[7] = 52;
        Files.createDirectories(out.getParent());
        Files.write(out, bytes);
    }
}
