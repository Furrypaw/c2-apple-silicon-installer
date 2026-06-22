import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public final class C2BassMusic {
    private static final String SETTINGS_FILE = "settings/music-enabled.txt";
    private static final Track[] TRACKS = {
            new Track("aztech_-_cosmic.mo3", "Aztech a.k.a. Toby", null, "Cosmic"),
            new Track("04-808rmx.mo3", "bay tremore", null, "04-808rmx"),
            new Track("11-kobnik.mo3", "prob", null, "kob nik-bonus track"),
            new Track("a&s-hark.mo3", "assign and salice", null, "harke"),
            new Track("agn-unle.mo3", "assign and salice", null, "unleash"),
            new Track("anita.mo3", "mortimer twang", null, "anita"),
            new Track("atom.mo3", "dreamfish", null, "atom-sphere"),
            new Track("bangormx.mo3", "bay tremore", null, "bango (bt d&b remix)"),
            new Track("bonustrk.mo3", "esa ruoho", "http://www.lackluster.org/", "untitled bonus track"),
            new Track("c-crab.mo3", "Cactus", null, "The Crab Temple (HT)"),
            new Track("chatter.mo3", "Bassline", null, "Chattering"),
            new Track("drumbass.mo3", "substance", null, "drum^bass_frenzie"),
            new Track("echtzeit.mo3", "Jazz/Haujobb", null, "echtzeit-rmx"),
            new Track("fern.mo3", "styl", null, "fern pollen"),
            new Track("flow.mo3", "mortimer twang", null, "flow"),
            new Track("kri#g4e.mo3", "krii", null, "gone forever"),
            new Track("kri#simd.mo3", "krii", null, "shapes in motion"),
            new Track("lia.mo3", "muffler", null, "lia"),
            new Track("material.mo3", "fender", null, "material flows"),
            new Track("mtz_beha.mo3", "mentz (1998)", null, "bread of haste"),
            new Track("mtz_keep.mo3", "mentz (1998)", null, "keepin' it kool"),
            new Track("mtz_lash.mo3", "mentz (1998)", null, "last shadow"),
            new Track("mtz_onme.mo3", "mentz (1998)", null, "On me"),
            new Track("s-j9a.mo3", "SS", null, "Jungle tune #1"),
            new Track("sm-kalku.mo3", "stereoman", null, "keen until"),
            new Track("smoof.mo3", "Mefis", null, "connection busy"),
            new Track("steady.mo3", "bay tremore", null, "rockin' steady"),
            new Track("sweetkng.mo3", "mortimer twang", null, "s-w-e-e-t king mix"),
            new Track("tdrmix1.mo3", "TOWERX", null, "Tokyo D. Rcds mix 1"),
            new Track("true.mo3", "mortimer twang", null, "true"),
            new Track("twisted.mo3", "bay tremore", null, "twisted"),
            new Track("warm.mo3", "dreamfish", null, "warm"),
            new Track("tema1_1.mo3", "Victor Vergara Lujan", null, "tema1_1")
    };

    private static final Random RANDOM = new Random();
    private static boolean initialized;
    private static boolean available;
    private static boolean enabled;
    private static int currentHandle;
    private static int currentTrack = -1;

    private C2BassMusic() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        enabled = readEnabled();
        if (!enabled) {
            System.out.println("[C2 patch] BASS music disabled by " + SETTINGS_FILE);
            return;
        }
        try {
            File libsDir = new File("resources/libs-arm64");
            File bridge = new File(libsDir, "libC2BassMusic.jnilib");
            File bass = new File(libsDir, "libbass.dylib");
            if (!bridge.isFile() || !bass.isFile()) {
                System.out.println("[C2 patch] BASS music unavailable: missing arm64 bridge or libbass.dylib");
                return;
            }
            System.load(bridge.getAbsolutePath());
            available = nativeInit(bass.getAbsolutePath());
            if (available) {
                System.out.println("[C2 patch] BASS MO3 music enabled on Apple Silicon");
            } else {
                System.out.println("[C2 patch] BASS music init failed, error " + nativeError());
            }
        } catch (Throwable t) {
            System.out.println("[C2 patch] BASS music unavailable: " + t);
        }
    }

    public static synchronized void playMenuMusic() {
        playTrack(0);
    }

    public static synchronized void playByOffset(int offset) {
        int index = offset;
        if (offset < 0) {
            index = TRACKS.length + offset;
        }
        if (index < 0 || index >= TRACKS.length) {
            index = Math.abs(offset) % TRACKS.length;
        }
        playTrack(index);
    }

    public static synchronized void ensurePlaying() {
        if (available && enabled && currentHandle != 0 && !nativeIsActive(currentHandle)) {
            playTrack(RANDOM.nextInt(TRACKS.length));
        }
    }

    public static synchronized String currentUrl() {
        Track track = currentTrack();
        return track == null || track.url == null ? "" : track.url;
    }

    public static synchronized String currentArtist() {
        Track track = currentTrack();
        return track == null ? "" : track.artist;
    }

    public static synchronized String currentTitle() {
        Track track = currentTrack();
        return track == null ? "" : track.title;
    }

    public static synchronized Map catalog() {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        for (Track track : TRACKS) {
            map.put(track.artist + " - " + track.title, track.url == null ? "" : track.url);
        }
        return map;
    }

    public static synchronized void shutdown() {
        if (currentHandle != 0) {
            nativeStop(currentHandle);
            nativeFreeMusic(currentHandle);
            currentHandle = 0;
        }
        if (available) {
            nativeShutdown();
        }
        currentTrack = -1;
        available = false;
        initialized = false;
    }

    private static void playTrack(int index) {
        if (!available || !enabled || index < 0 || index >= TRACKS.length) {
            return;
        }
        try {
            if (currentHandle != 0) {
                nativeStop(currentHandle);
                nativeFreeMusic(currentHandle);
                currentHandle = 0;
            }
            File file = extractTrack(TRACKS[index]);
            int handle = nativeLoadMusic(file.getAbsolutePath(), true);
            if (handle == 0) {
                System.out.println("[C2 patch] Could not load music " + TRACKS[index].fileName + ", BASS error " + nativeError());
                return;
            }
            nativeSetVolume(handle, 0.45f);
            if (!nativePlay(handle, true)) {
                System.out.println("[C2 patch] Could not play music " + TRACKS[index].fileName + ", BASS error " + nativeError());
                nativeFreeMusic(handle);
                return;
            }
            currentHandle = handle;
            currentTrack = index;
        } catch (Throwable t) {
            System.out.println("[C2 patch] Music playback failed: " + t);
        }
    }

    private static Track currentTrack() {
        if (currentTrack < 0 || currentTrack >= TRACKS.length) {
            return null;
        }
        return TRACKS[currentTrack];
    }

    private static boolean readEnabled() {
        File file = new File(SETTINGS_FILE);
        if (!file.isFile()) {
            return true;
        }
        InputStream in = null;
        try {
            in = new java.io.FileInputStream(file);
            String value = new String(readAll(in), "UTF-8").trim().toLowerCase();
            return !(value.equals("0") || value.equals("false") || value.equals("off")
                    || value.equals("no") || value.equals("disabled"));
        } catch (Exception e) {
            return true;
        } finally {
            closeQuietly(in);
        }
    }

    private static File extractTrack(Track track) throws Exception {
        File dir = new File("resources/music-cache");
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        File out = new File(dir, track.fileName);
        if (out.isFile() && out.length() > 0) {
            return out;
        }
        InputStream in = C2BassMusic.class.getResourceAsStream("/data/" + track.fileName);
        if (in == null) {
            throw new java.io.FileNotFoundException(track.fileName);
        }
        FileOutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream(out);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                fileOut.write(buffer, 0, read);
            }
        } finally {
            closeQuietly(fileOut);
            closeQuietly(in);
        }
        return out;
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
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
            Method close = closeable.getClass().getMethod("close");
            close.invoke(closeable);
        } catch (Throwable ignored) {
        }
    }

    private static native boolean nativeInit(String bassPath);
    private static native int nativeLoadMusic(String path, boolean loop);
    private static native boolean nativePlay(int handle, boolean restart);
    private static native boolean nativeStop(int handle);
    private static native boolean nativeFreeMusic(int handle);
    private static native boolean nativeSetVolume(int handle, float volume);
    private static native boolean nativeIsActive(int handle);
    private static native void nativeShutdown();
    private static native int nativeError();

    private static final class Track {
        final String fileName;
        final String artist;
        final String url;
        final String title;

        Track(String fileName, String artist, String url, String title) {
            this.fileName = fileName;
            this.artist = artist;
            this.url = url;
            this.title = title;
        }
    }
}
