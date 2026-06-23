import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class PatchMusicVolumeSettingHook {
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
                if (name.equals("method827") && desc.equals("(Ljava/lang/String;I)V")) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                    mv.visitCode();
                    mv.visitFieldInsn(Opcodes.GETSTATIC, "JB_129", "B", "Ljava/util/HashMap;");
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/HashMap", "containsKey", "(Ljava/lang/Object;)Z", false);
                    Label knownKey = new Label();
                    mv.visitJumpInsn(Opcodes.IFNE, knownKey);
                    mv.visitTypeInsn(Opcodes.NEW, "java/lang/RuntimeException");
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "()V", false);
                    mv.visitInsn(Opcodes.ATHROW);
                    mv.visitLabel(knownKey);
                    mv.visitFieldInsn(Opcodes.GETSTATIC, "JB_129", "D", "Ljava/util/HashMap;");
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitVarInsn(Opcodes.ILOAD, 1);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/HashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
                    mv.visitInsn(Opcodes.POP);
                    mv.visitLdcInsn("s");
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
                    Label done = new Label();
                    mv.visitJumpInsn(Opcodes.IFEQ, done);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "C2BassMusic", "updateVolumeNow", "()V", false);
                    mv.visitLabel(done);
                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                    return null;
                }
                return super.visitMethod(access, name, desc, signature, exceptions);
            }
        };
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        Files.createDirectories(out.getParent());
        Files.write(out, cw.toByteArray());
    }
}
