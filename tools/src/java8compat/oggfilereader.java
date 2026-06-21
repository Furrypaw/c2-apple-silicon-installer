import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class oggfilereader {
    public static String getNthOggFile(int targetLine) {
        try (BufferedReader reader = new BufferedReader(new FileReader("settings/UE-oggfiles.txt"))) {
            String line;
            int currentLine = 1;
            while ((line = reader.readLine()) != null) {
                if (currentLine == targetLine) {
                    return line;
                }
                currentLine++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
