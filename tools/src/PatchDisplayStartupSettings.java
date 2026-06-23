import java.nio.file.*;
import org.objectweb.asm.*;

public class PatchDisplayStartupSettings {
    public static void main(String[] args) throws Exception {
        Path in = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        ClassReader cr = new ClassReader(Files.readAllBytes(in));
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                return "java/lang/Object";
            }
        };
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if (name.equals("method26") && desc.equals("()V")) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                    emitMethod26(mv);
                    return null;
                }
                return super.visitMethod(access, name, desc, signature, exceptions);
            }

            private void emitMethod26(MethodVisitor mv) {
                Label notFullscreen = new Label();
                Label fullscreenDone = new Label();
                Label usePrefsSize = new Label();
                Label sizeDone = new Label();

                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);

                mv.visitFieldInsn(Opcodes.GETSTATIC, "net/gewaltig/cultris/Cultris", "field2393", "Z");
                mv.visitJumpInsn(Opcodes.IFNE, notFullscreen);
                mv.visitLdcInsn("l");
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "JB_129", "method831", "(Ljava/lang/String;)I", false);
                mv.visitJumpInsn(Opcodes.IFEQ, notFullscreen);
                mv.visitInsn(Opcodes.ICONST_1);
                mv.visitJumpInsn(Opcodes.GOTO, fullscreenDone);
                mv.visitLabel(notFullscreen);
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitLabel(fullscreenDone);

                mv.visitLdcInsn("m");
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "JB_129", "method831", "(Ljava/lang/String;)I", false);

                mv.visitFieldInsn(Opcodes.GETSTATIC, "net/gewaltig/cultris/Cultris", "field2393", "Z");
                mv.visitJumpInsn(Opcodes.IFEQ, usePrefsSize);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/Display", "while", "()Ljava/awt/Canvas;", false);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/awt/Canvas", "getWidth", "()I", false);
                mv.visitJumpInsn(Opcodes.GOTO, sizeDone);
                mv.visitLabel(usePrefsSize);
                mv.visitLdcInsn("k");
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "JB_129", "method831", "(Ljava/lang/String;)I", false);
                mv.visitLabel(sizeDone);

                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "C2DisplaySettings", "apply", "(LFE_76;ZII)Z", false);
                mv.visitInsn(Opcodes.POP);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        };
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        Files.createDirectories(out.getParent());
        Files.write(out, cw.toByteArray());
    }
}
