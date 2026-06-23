import java.nio.file.*;
import org.objectweb.asm.*;

public class PatchDisplayForceWindowed {
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
                if (name.equals("method2981") && desc.equals("(Lorg/lwjgl/opengl/PixelFormat;Lorg/lwjgl/opengl/k_886;Lorg/lwjgl/opengl/CB_726;)V")) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode == Opcodes.RETURN) {
                                super.visitInsn(Opcodes.ICONST_1);
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/Display", "method3040", "(Z)V", false);
                            }
                            super.visitInsn(opcode);
                        }
                    };
                }
                if (name.equals("method3040") && desc.equals("(Z)V")) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                    mv.visitCode();
                    mv.visitVarInsn(Opcodes.ILOAD, 0);
                    mv.visitFieldInsn(Opcodes.PUTSTATIC, "org/lwjgl/opengl/Display", "field3277", "Z");
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/Display", "method3019", "()Z", false);
                    Label done = new Label();
                    mv.visitJumpInsn(Opcodes.IFEQ, done);
                    mv.visitFieldInsn(Opcodes.GETSTATIC, "org/lwjgl/opengl/Display", "field3290", "Lorg/lwjgl/opengl/m_893;");
                    mv.visitVarInsn(Opcodes.ILOAD, 0);
                    mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/lwjgl/opengl/m_893", "method4721", "(Z)V", true);
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
