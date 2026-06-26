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
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                return "java/lang/Object";
            }
        };
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if (name.equals("method4708") && desc.equals("(Lorg/lwjgl/opengl/C_724;Lorg/lwjgl/opengl/DisplayMode;Ljava/awt/Canvas;II)V")) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        private boolean skippingOldPropertyCheck;

                        @Override
                        public void visitLdcInsn(Object value) {
                            if ("c2.disableWindowResize".equals(value)) {
                                super.visitInsn(Opcodes.ICONST_0);
                                skippingOldPropertyCheck = true;
                                return;
                            }
                            super.visitLdcInsn(value);
                        }

                        @Override
                        public void visitVarInsn(int opcode, int var) {
                            if (skippingOldPropertyCheck) {
                                if (opcode == Opcodes.ILOAD && var == 8) {
                                    skippingOldPropertyCheck = false;
                                    super.visitVarInsn(opcode, var);
                                }
                                return;
                            }
                            if (opcode == Opcodes.ILOAD && var == 7) {
                                super.visitInsn(Opcodes.ICONST_0);
                                return;
                            }
                            super.visitVarInsn(opcode, var);
                        }

                        @Override public void visitInsn(int opcode) {
                            if (!skippingOldPropertyCheck) super.visitInsn(opcode);
                        }
                        @Override public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                            if (!skippingOldPropertyCheck) super.visitMethodInsn(opcode, owner, name, desc, itf);
                        }
                        @Override public void visitJumpInsn(int opcode, Label label) {
                            if (!skippingOldPropertyCheck) super.visitJumpInsn(opcode, label);
                        }
                        @Override public void visitLabel(Label label) {
                            if (!skippingOldPropertyCheck) super.visitLabel(label);
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
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        Files.createDirectories(out.getParent());
        Files.write(out, cw.toByteArray());
    }
}
