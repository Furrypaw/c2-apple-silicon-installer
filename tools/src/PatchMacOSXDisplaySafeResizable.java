import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class PatchMacOSXDisplaySafeResizable {
    public static void main(String[] args) throws Exception {
        Path in = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        ClassReader cr = new ClassReader(Files.readAllBytes(in));
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if (name.equals("method4708") && desc.equals("(Lorg/lwjgl/opengl/C_724;Lorg/lwjgl/opengl/DisplayMode;Ljava/awt/Canvas;II)V")) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitVarInsn(int opcode, int var) {
                            if (opcode == Opcodes.ILOAD && var == 7) {
                                Label enabled = new Label();
                                Label done = new Label();
                                super.visitLdcInsn("c2.disableWindowResize");
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "getBoolean", "(Ljava/lang/String;)Z", false);
                                super.visitJumpInsn(Opcodes.IFEQ, enabled);
                                super.visitInsn(Opcodes.ICONST_0);
                                super.visitJumpInsn(Opcodes.GOTO, done);
                                super.visitLabel(enabled);
                                super.visitInsn(Opcodes.ICONST_1);
                                super.visitLabel(done);
                                return;
                            }
                            super.visitVarInsn(opcode, var);
                        }
                    };
                }
                if (name.equals("method4721") && desc.equals("(Z)V")) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                    mv.visitCode();
                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                    return null;
                }
                return super.visitMethod(access, name, desc, signature, exceptions);
            }
        };
        cr.accept(cv, 0);
        Files.createDirectories(out.getParent());
        Files.write(out, cw.toByteArray());
    }
}
