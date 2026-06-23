import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public final class C2DisplaySettings {
    private static final String FULLSCREEN_FILE = "settings/display-fullscreen.txt";
    private static final String WINDOW_SIZE_FILE = "settings/display-window-size.txt";

    private C2DisplaySettings() {
    }

    public static boolean apply(FE_76 display, boolean defaultFullscreen, int defaultDisplayMode, int defaultWindowSize) {
        boolean fullscreen = readBoolean(FULLSCREEN_FILE, defaultFullscreen);
        int displayMode = Math.max(0, defaultDisplayMode);
        int windowSize = readInt(WINDOW_SIZE_FILE, defaultWindowSize);
        if (windowSize <= 0) {
            windowSize = defaultWindowSize;
        }
        windowSize = clamp(windowSize, 320, 2400);
        return display.method452(fullscreen, displayMode, windowSize);
    }

    private static boolean readBoolean(String path, boolean fallback) {
        String value = readFirstLine(path);
        if (value == null || value.length() == 0 || "default".equalsIgnoreCase(value)) {
            return fallback;
        }
        return "1".equals(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }

    private static int readInt(String path, int fallback) {
        String value = readFirstLine(path);
        if (value == null || value.length() == 0 || "default".equalsIgnoreCase(value)) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String readFirstLine(String path) {
        File file = new File(path);
        if (!file.isFile()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            return line == null ? null : line.trim();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
