import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

public class RemapArmMacLWJGL {
    private static final Map<String, String> classes = new HashMap<String, String>();
    private static final Map<String, String> methods = new HashMap<String, String>();
    private static final Map<String, String> fields = new HashMap<String, String>();

    static {
        cls("org/lwjgl/PointerBuffer", "org/lwjgl/h_695");
        cls("org/lwjgl/opengl/DisplayImplementation", "org/lwjgl/opengl/m_893");
        cls("org/lwjgl/opengl/InputImplementation", "org/lwjgl/opengl/i_875");
        cls("org/lwjgl/opengl/Drawable", "org/lwjgl/opengl/k_886");
        cls("org/lwjgl/opengl/DrawableLWJGL", "org/lwjgl/opengl/C_724");
        cls("org/lwjgl/opengl/DrawableGL", "org/lwjgl/opengl/q_915");
        cls("org/lwjgl/opengl/PeerInfo", "org/lwjgl/opengl/a_835");
        cls("org/lwjgl/opengl/ContextAttribs", "org/lwjgl/opengl/CB_726");
        cls("org/lwjgl/opengl/ContextGL", "org/lwjgl/opengl/Jb_756");
        cls("org/lwjgl/opengl/ContextImplementation", "org/lwjgl/opengl/M_768");

        // Display static API used by the newer Mac backend.
        m("org/lwjgl/opengl/Display", "getDrawable", "()Lorg/lwjgl/opengl/Drawable;", "method2999");
        m("org/lwjgl/opengl/Display", "getPrivilegedBoolean", "(Ljava/lang/String;)Z", "method3044");
        m("org/lwjgl/opengl/Display", "isFullscreen", "()Z", "throw");
        m("org/lwjgl/opengl/Display", "isResizable", "()Z", "method3021");
        m("org/lwjgl/opengl/Display", "isCreated", "()Z", "method3019");
        m("org/lwjgl/opengl/Display", "getDesktopDisplayMode", "()Lorg/lwjgl/opengl/DisplayMode;", "method3011");
        m("org/lwjgl/opengl/Display", "getParent", "()Ljava/awt/Canvas;", "while");
        m("org/lwjgl/opengl/Display", "getImplementation", "()Lorg/lwjgl/opengl/DisplayImplementation;", "getImplementation");

        // DisplayMode accessors.
        m("org/lwjgl/opengl/DisplayMode", "getWidth", "()I", "method3047");
        m("org/lwjgl/opengl/DisplayMode", "getHeight", "()I", "method3049");
        m("org/lwjgl/opengl/DisplayMode", "getBitsPerPixel", "()I", "method3045");
        m("org/lwjgl/opengl/DisplayMode", "getFrequency", "()I", "method3046");
        m("org/lwjgl/opengl/DisplayMode", "isFullscreenCapable", "()Z", "method3048");

        // DisplayImplementation -> m_893.
        m("org/lwjgl/opengl/MacOSXDisplay", "createWindow", "(Lorg/lwjgl/opengl/DrawableLWJGL;Lorg/lwjgl/opengl/DisplayMode;Ljava/awt/Canvas;II)V", "method4708");
        m("org/lwjgl/opengl/MacOSXDisplay", "destroyWindow", "()V", "method4718");
        m("org/lwjgl/opengl/MacOSXDisplay", "switchDisplayMode", "(Lorg/lwjgl/opengl/DisplayMode;)V", "method4730");
        m("org/lwjgl/opengl/MacOSXDisplay", "resetDisplayMode", "()V", "method4733");
        m("org/lwjgl/opengl/MacOSXDisplay", "getGammaRampLength", "()I", "method4750");
        m("org/lwjgl/opengl/MacOSXDisplay", "getAdapter", "()Ljava/lang/String;", "method4728");
        m("org/lwjgl/opengl/MacOSXDisplay", "getVersion", "()Ljava/lang/String;", "method4716");
        m("org/lwjgl/opengl/MacOSXDisplay", "init", "()Lorg/lwjgl/opengl/DisplayMode;", "method4698");
        m("org/lwjgl/opengl/MacOSXDisplay", "setTitle", "(Ljava/lang/String;)V", "method4736");
        m("org/lwjgl/opengl/MacOSXDisplay", "isCloseRequested", "()Z", "false");
        m("org/lwjgl/opengl/MacOSXDisplay", "isVisible", "()Z", "method4775");
        m("org/lwjgl/opengl/MacOSXDisplay", "isActive", "()Z", "method4743");
        m("org/lwjgl/opengl/MacOSXDisplay", "isDirty", "()Z", "method4714");
        m("org/lwjgl/opengl/MacOSXDisplay", "createPeerInfo", "(Lorg/lwjgl/opengl/PixelFormat;Lorg/lwjgl/opengl/ContextAttribs;)Lorg/lwjgl/opengl/PeerInfo;", "method4717");
        m("org/lwjgl/opengl/MacOSXDisplay", "update", "()V", "method4702");
        m("org/lwjgl/opengl/MacOSXDisplay", "reshape", "(IIII)V", "method4734");
        m("org/lwjgl/opengl/MacOSXDisplay", "getPbufferCapabilities", "()I", "method4715");
        m("org/lwjgl/opengl/MacOSXDisplay", "isBufferLost", "(Lorg/lwjgl/opengl/PeerInfo;)Z", "method4705");
        m("org/lwjgl/opengl/MacOSXDisplay", "createPbuffer", "(IILorg/lwjgl/opengl/PixelFormat;Lorg/lwjgl/opengl/ContextAttribs;Ljava/nio/IntBuffer;Ljava/nio/IntBuffer;)Lorg/lwjgl/opengl/PeerInfo;", "method4701");
        m("org/lwjgl/opengl/MacOSXDisplay", "setPbufferAttrib", "(Lorg/lwjgl/opengl/PeerInfo;II)V", "method4738");
        m("org/lwjgl/opengl/MacOSXDisplay", "bindTexImageToPbuffer", "(Lorg/lwjgl/opengl/PeerInfo;I)V", "method4731");
        m("org/lwjgl/opengl/MacOSXDisplay", "releaseTexImageFromPbuffer", "(Lorg/lwjgl/opengl/PeerInfo;I)V", "method4776");
        m("org/lwjgl/opengl/MacOSXDisplay", "setIcon", "([Ljava/nio/ByteBuffer;)I", "method4752");
        m("org/lwjgl/opengl/MacOSXDisplay", "setResizable", "(Z)V", "method4721");
        m("org/lwjgl/opengl/MacOSXDisplay", "wasResized", "()Z", "method4751");
        m("org/lwjgl/opengl/MacOSXDisplay", "getWidth", "()I", "method4747");
        m("org/lwjgl/opengl/MacOSXDisplay", "getHeight", "()I", "method4780");
        m("org/lwjgl/opengl/MacOSXDisplay", "getX", "()I", "method4704");
        m("org/lwjgl/opengl/MacOSXDisplay", "getY", "()I", "true");

        // InputImplementation -> i_875.
        m("org/lwjgl/opengl/MacOSXDisplay", "hasWheel", "()Z", "method4762");
        m("org/lwjgl/opengl/MacOSXDisplay", "getButtonCount", "()I", "new");
        m("org/lwjgl/opengl/MacOSXDisplay", "createMouse", "()V", "this");
        m("org/lwjgl/opengl/MacOSXDisplay", "destroyMouse", "()V", "method4769");
        m("org/lwjgl/opengl/MacOSXDisplay", "pollMouse", "(Ljava/nio/IntBuffer;Ljava/nio/ByteBuffer;)V", "method4767");
        m("org/lwjgl/opengl/MacOSXDisplay", "readMouse", "(Ljava/nio/ByteBuffer;)V", "method4703");
        m("org/lwjgl/opengl/MacOSXDisplay", "grabMouse", "(Z)V", "method4758");
        m("org/lwjgl/opengl/MacOSXDisplay", "getNativeCursorCapabilities", "()I", "try");
        m("org/lwjgl/opengl/MacOSXDisplay", "setCursorPosition", "(II)V", "method4778");
        m("org/lwjgl/opengl/MacOSXDisplay", "setNativeCursor", "(Ljava/lang/Object;)V", "method4709");
        m("org/lwjgl/opengl/MacOSXDisplay", "getMinCursorSize", "()I", "method4763");
        m("org/lwjgl/opengl/MacOSXDisplay", "getMaxCursorSize", "()I", "method4744");
        m("org/lwjgl/opengl/MacOSXDisplay", "createKeyboard", "()V", "method4707");
        m("org/lwjgl/opengl/MacOSXDisplay", "destroyKeyboard", "()V", "method4724");
        m("org/lwjgl/opengl/MacOSXDisplay", "pollKeyboard", "(Ljava/nio/ByteBuffer;)V", "method4710");
        m("org/lwjgl/opengl/MacOSXDisplay", "readKeyboard", "(Ljava/nio/ByteBuffer;)V", "method4777");
        m("org/lwjgl/opengl/MacOSXDisplay", "createCursor", "(IIIIILjava/nio/IntBuffer;Ljava/nio/IntBuffer;)Ljava/lang/Object;", "method4713");
        m("org/lwjgl/opengl/MacOSXDisplay", "destroyCursor", "(Ljava/lang/Object;)V", "method4772");
        m("org/lwjgl/opengl/MacOSXDisplay", "isInsideWindow", "()Z", "method4748");

        // PeerInfo abstract/base methods.
        m("org/lwjgl/opengl/PeerInfo", "unlock", "()V", "method4676");
        m("org/lwjgl/opengl/PeerInfo", "doLockAndInitHandle", "()V", "method4672");
        m("org/lwjgl/opengl/PeerInfo", "doUnlock", "()V", "method4671");
        m("org/lwjgl/opengl/PeerInfo", "lockAndGetHandle", "()Ljava/nio/ByteBuffer;", "method4679");
        m("org/lwjgl/opengl/PeerInfo", "getHandle", "()Ljava/nio/ByteBuffer;", "method4677");
        m("org/lwjgl/opengl/PeerInfo", "destroy", "()V", "method4675");
        for (String owner : new String[] {
                "org/lwjgl/opengl/MacOSXPeerInfo",
                "org/lwjgl/opengl/MacOSXCanvasPeerInfo",
                "org/lwjgl/opengl/MacOSXDisplayPeerInfo",
                "org/lwjgl/opengl/MacOSXPbufferPeerInfo" }) {
            m(owner, "unlock", "()V", "method4676");
            m(owner, "doLockAndInitHandle", "()V", "method4672");
            m(owner, "doUnlock", "()V", "method4671");
            m(owner, "lockAndGetHandle", "()Ljava/nio/ByteBuffer;", "method4679");
            m(owner, "getHandle", "()Ljava/nio/ByteBuffer;", "method4677");
            m(owner, "destroy", "()V", "method4675");
        }

        // ContextImplementation -> M_768.
        m("org/lwjgl/opengl/MacOSXContextImplementation", "create", "(Lorg/lwjgl/opengl/PeerInfo;Ljava/nio/IntBuffer;Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;", "method4696");
        m("org/lwjgl/opengl/MacOSXContextImplementation", "swapBuffers", "()V", "method4690");
        m("org/lwjgl/opengl/MacOSXContextImplementation", "releaseDrawable", "(Ljava/nio/ByteBuffer;)V", "method4691");
        m("org/lwjgl/opengl/MacOSXContextImplementation", "releaseCurrentContext", "()V", "method4693");
        m("org/lwjgl/opengl/MacOSXContextImplementation", "update", "(Ljava/nio/ByteBuffer;)V", "method4694");
        m("org/lwjgl/opengl/MacOSXContextImplementation", "makeCurrent", "(Lorg/lwjgl/opengl/PeerInfo;Ljava/nio/ByteBuffer;)V", "method4692");
        m("org/lwjgl/opengl/MacOSXContextImplementation", "isCurrent", "(Ljava/nio/ByteBuffer;)Z", "method4688");
        m("org/lwjgl/opengl/MacOSXContextImplementation", "setSwapInterval", "(I)V", "method4695");
        m("org/lwjgl/opengl/MacOSXContextImplementation", "destroy", "(Lorg/lwjgl/opengl/PeerInfo;Ljava/nio/ByteBuffer;)V", "method4689");
        m("org/lwjgl/opengl/MacOSXContextImplementation", "resetView", "(Lorg/lwjgl/opengl/PeerInfo;Lorg/lwjgl/opengl/ContextGL;)V", "method4697");

        // ContextGL refs used by the context implementation.
        m("org/lwjgl/opengl/ContextGL", "getCurrentContext", "()Lorg/lwjgl/opengl/ContextGL;", "method3632");
        m("org/lwjgl/opengl/ContextGL", "getHandle", "()Ljava/nio/ByteBuffer;", "method3629");
        m("org/lwjgl/opengl/ContextGL", "update", "()V", "method3627");

        // ContextAttribs getters used by Mac peer info. These are rarely non-null for Cultris.
        m("org/lwjgl/opengl/ContextAttribs", "getMajorVersion", "()I", "this");
        m("org/lwjgl/opengl/ContextAttribs", "getMinorVersion", "()I", "method2964");
        m("org/lwjgl/opengl/ContextAttribs", "isProfileCore", "()Z", "method2959");

        // GL11 helpers used by the Mac display update path.
        m("org/lwjgl/opengl/GL11", "glGetInteger", "(ILjava/nio/IntBuffer;)V", "method3688");
        m("org/lwjgl/opengl/GL11", "glViewport", "(IIII)V", "method3865");

        // Utility calls used by the native-window backend.
        m("org/lwjgl/MemoryUtil", "encodeUTF8", "(Ljava/lang/CharSequence;)Ljava/nio/ByteBuffer;", "method1847");

        f("org/lwjgl/opengl/DrawableGL", "peer_info", "Lorg/lwjgl/opengl/PeerInfo;", "field510");
        f("org/lwjgl/opengl/DrawableGL", "context", "Lorg/lwjgl/opengl/ContextGL;", "field509");
    }

    private static void cls(String a, String b) { classes.put(a, b); }
    private static void m(String owner, String name, String desc, String mapped) { methods.put(owner + "." + name + desc, mapped); }
    private static void f(String owner, String name, String desc, String mapped) { fields.put(owner + "." + name + desc, mapped); }

    public static void main(String[] args) throws Exception {
        Path inJar = Paths.get(args[0]);
        Path outDir = Paths.get(args[1]);
        Files.createDirectories(outDir);
        final Set<String> wanted = new HashSet<String>(Arrays.asList(
            "org/lwjgl/opengl/AWTUtil$1.class",
            "org/lwjgl/opengl/AWTUtil$2.class",
            "org/lwjgl/opengl/AWTUtil.class",
            "org/lwjgl/opengl/EventQueue.class",
            "org/lwjgl/opengl/KeyboardEventQueue.class",
            "org/lwjgl/opengl/MacOSXCanvasPeerInfo$1.class",
            "org/lwjgl/opengl/MacOSXCanvasPeerInfo.class",
            "org/lwjgl/opengl/MacOSXContextImplementation.class",
            "org/lwjgl/opengl/MacOSXDisplay.class",
            "org/lwjgl/opengl/MacOSXDisplayPeerInfo.class",
            "org/lwjgl/opengl/MacOSXGLCanvas.class",
            "org/lwjgl/opengl/MacOSXMouseEventQueue.class",
            "org/lwjgl/opengl/MacOSXNativeKeyboard.class",
            "org/lwjgl/opengl/MacOSXNativeMouse.class",
            "org/lwjgl/opengl/MacOSXPbufferPeerInfo.class",
            "org/lwjgl/opengl/MacOSXPeerInfo.class",
            "org/lwjgl/opengl/MouseEventQueue.class"
        ));
        try (JarFile jar = new JarFile(inJar.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                if (!wanted.contains(e.getName())) continue;
                ClassReader cr = new ClassReader(jar.getInputStream(e));
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
                    @Override
                    protected String getCommonSuperClass(String type1, String type2) {
                        return "java/lang/Object";
                    }
                };
                ClassVisitor cv = new ClassRemapper(cw, new Remapper() {
                    @Override public String map(String typeName) {
                        String v = classes.get(typeName);
                        return v == null ? typeName : v;
                    }
                    @Override public String mapMethodName(String owner, String name, String desc) {
                        String v = methods.get(owner + "." + name + desc);
                        return v == null ? name : v;
                    }
                    @Override public String mapFieldName(String owner, String name, String desc) {
                        String v = fields.get(owner + "." + name + desc);
                        return v == null ? name : v;
                    }
                });
                cr.accept(cv, ClassReader.EXPAND_FRAMES);
                ClassReader out = new ClassReader(cw.toByteArray());
                Path target = outDir.resolve(out.getClassName() + ".class");
                Files.createDirectories(target.getParent());
                Files.write(target, cw.toByteArray());
                System.out.println(e.getName() + " -> " + out.getClassName());
            }
        }
    }
}
