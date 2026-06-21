import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ReadTeamColor {
    private static final String TEAM_COLORS_FILE = "settings/team-colors.txt";

    public static String getTeamName(int index) {
        String[] parts = getParts(index);
        return parts != null ? parts[0].trim() : null;
    }

    public static float getRed(int index) {
        float[] color = getColor(index);
        return color != null ? color[0] : 0.0f;
    }

    public static float getGreen(int index) {
        float[] color = getColor(index);
        return color != null ? color[1] : 0.0f;
    }

    public static float getBlue(int index) {
        float[] color = getColor(index);
        return color != null ? color[2] : 0.0f;
    }

    public static void main(String[] args) {
    }

    private static float[] getColor(int index) {
        String[] parts = getParts(index);
        if (parts == null || parts.length != 4) {
            return null;
        }
        try {
            return new float[] {
                    normalize(Float.parseFloat(parts[1].trim())),
                    normalize(Float.parseFloat(parts[2].trim())),
                    normalize(Float.parseFloat(parts[3].trim()))
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String[] getParts(int index) {
        try (BufferedReader reader = new BufferedReader(new FileReader(TEAM_COLORS_FILE))) {
            String line = null;
            for (int i = 0; i <= index; i++) {
                line = reader.readLine();
                if (line == null) {
                    return null;
                }
            }
            String[] parts = line.split(",");
            return parts.length == 4 ? parts : null;
        } catch (IOException e) {
            System.err.println("Error reading the team color file: " + e.getMessage());
            return null;
        }
    }

    private static float normalize(float value) {
        return value > 1.0f ? value / 255.0f : value;
    }
}
