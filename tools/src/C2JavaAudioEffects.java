import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

public final class C2JavaAudioEffects {
    private static final String SETTINGS_FILE = "settings/UE-oggfiles.txt";
    private static final String[] DEFAULT_SOUNDS = {
            "exp-04-menu-bing.ogg",
            "exp-01-menu-woosh.ogg",
            "exp-07-book.ogg",
            "exp-08-zap-error.ogg",
            "exp-09-round-countdown.ogg",
            "exp-10-round-start.ogg",
            "exp-16-harddrop-pure.ogg",
            "exp-15-harddrop-pure-effect.ogg",
            "exp-05-drop-effect.ogg",
            "exp-02-attack-effect.ogg",
            "exp-03-lines-in-end.ogg",
            "exp-06-glass-shatter.ogg",
            "exp-11-restricted-bpm-eat.ogg",
            "exp-12-impressive.ogg",
            "exp-13-perfect.ogg",
            "exp-14-godlike.ogg"
    };

    private static final int[] LINE_BY_ORDINAL = {
            7, 8, 5, 6, 4, 14, 15, 16, 9, 10, 11, 12, 13, 1, 3, 2
    };
    private static final Map<Object, Sound> SOUNDS = new ConcurrentHashMap<Object, Sound>();
    private static volatile boolean initialized;

    private C2JavaAudioEffects() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        int loaded = 0;
        try {
            String[] configured = readSoundList();
            Method valuesMethod = Class.forName("zg_1112").getMethod("values");
            Object[] values = (Object[]) valuesMethod.invoke(null);
            for (Object value : values) {
                int ordinal = ((Enum) value).ordinal();
                if (ordinal < 0 || ordinal >= LINE_BY_ORDINAL.length) {
                    continue;
                }
                int line = LINE_BY_ORDINAL[ordinal];
                if (line <= 0 || line > configured.length) {
                    continue;
                }
                String name = configured[line - 1];
                if (name == null || name.trim().length() == 0 || name.startsWith("disabled_")) {
                    continue;
                }
                Sound sound = loadSound(name);
                if (sound != null) {
                    SOUNDS.put(value, sound);
                    loaded++;
                }
            }
            System.out.println("[C2 patch] Java audio effects enabled on Apple Silicon (" + loaded + " sounds)");
            System.out.println("[C2 patch] BASS/MO3 music remains disabled to keep the game arm64-native.");
        } catch (Throwable t) {
            System.out.println("[C2 patch] Java audio effects unavailable: " + t);
        }
    }

    public static void play(Object soundKey, float volume) {
        if (volume <= 0.0f) {
            return;
        }
        Sound sound = SOUNDS.get(soundKey);
        if (sound == null) {
            return;
        }
        sound.play(volume);
    }

    public static synchronized void shutdown() {
        for (Sound sound : SOUNDS.values()) {
            sound.close();
        }
        SOUNDS.clear();
        initialized = false;
    }

    private static String[] readSoundList() {
        List<String> lines = new ArrayList<String>();
        File file = new File(SETTINGS_FILE);
        if (file.isFile()) {
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                String content = new String(readAll(in), "UTF-8");
                String[] split = content.split("\\r?\\n");
                for (String line : split) {
                    lines.add(line.trim());
                }
            } catch (Exception e) {
                System.out.println("[C2 patch] Could not read " + SETTINGS_FILE + ": " + e);
            } finally {
                closeQuietly(in);
            }
        }
        while (lines.size() < DEFAULT_SOUNDS.length) {
            lines.add(DEFAULT_SOUNDS[lines.size()]);
        }
        return lines.toArray(new String[lines.size()]);
    }

    private static Sound loadSound(String configuredName) {
        String name = configuredName.trim();
        String wavName = name.endsWith(".ogg") ? name.substring(0, name.length() - 4) + ".wav" : name;
        InputStream in = C2JavaAudioEffects.class.getResourceAsStream("/data/" + wavName);
        if (in == null) {
            in = C2JavaAudioEffects.class.getResourceAsStream("/data/" + name);
        }
        if (in == null) {
            System.out.println("[C2 patch] Missing sound resource: " + configuredName);
            return null;
        }
        try {
            return Sound.load(new BufferedInputStream(in));
        } catch (Throwable t) {
            System.out.println("[C2 patch] Could not load sound " + configuredName + ": " + t);
            return null;
        } finally {
            closeQuietly(in);
        }
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static void closeQuietly(Object closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.getClass().getMethod("close").invoke(closeable);
        } catch (Throwable ignored) {
        }
    }

    private static final class Sound {
        private static final int CLIP_COUNT = 4;
        private final Clip[] clips;
        private int nextClip;

        private Sound(Clip[] clips) {
            this.clips = clips;
        }

        static Sound load(InputStream in) throws Exception {
            AudioInputStream source = AudioSystem.getAudioInputStream(in);
            AudioFormat baseFormat = source.getFormat();
            AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false);
            AudioInputStream decoded = AudioSystem.getAudioInputStream(decodedFormat, source);
            byte[] data = readAll(decoded);
            closeQuietly(decoded);
            closeQuietly(source);

            Clip[] clips = new Clip[CLIP_COUNT];
            for (int i = 0; i < clips.length; i++) {
                clips[i] = AudioSystem.getClip();
                clips[i].open(decodedFormat, data, 0, data.length);
            }
            return new Sound(clips);
        }

        synchronized void play(float volume) {
            Clip clip = clips[nextClip];
            nextClip = (nextClip + 1) % clips.length;
            if (clip.isRunning()) {
                clip.stop();
            }
            setVolume(clip, volume);
            clip.setFramePosition(0);
            clip.start();
        }

        synchronized void close() {
            for (Clip clip : clips) {
                clip.close();
            }
        }

        private void setVolume(Clip clip, float volume) {
            if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                return;
            }
            FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float clamped = Math.max(0.01f, Math.min(1.0f, volume));
            float gain = (float) (20.0d * Math.log10(clamped));
            gain = Math.max(control.getMinimum(), Math.min(control.getMaximum(), gain));
            control.setValue(gain);
        }
    }
}
