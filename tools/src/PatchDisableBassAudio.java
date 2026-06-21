import java.nio.file.*;
import org.objectweb.asm.*;

public class PatchDisableBassAudio {
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
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if (name.equals("try") && desc.equals("()V")) {
                    emitInitNoop(mv);
                    return null;
                }
                if ((name.equals("method14") && desc.equals("(I)V"))
                        || (name.equals("method89") && desc.equals("()V"))
                        || (name.equals("method61") && desc.equals("()V"))
                        || (name.equals("method422") && desc.equals("(Lzg_1112;F)V"))
                        || (name.equals("method423") && desc.equals("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"))
                        || (name.equals("method424") && desc.equals("(Lzg_1112;Ljava/lang/String;LVf_301;F)V"))
                        || (name.equals("method56") && desc.equals("()V"))
                        || (name.equals("readoggfileslist") && desc.equals("()V"))) {
                    emitReturn(mv);
                    return null;
                }
                if ((name.equals("method420") || name.equals("method421") || name.equals("method356")
                        || name.equals("method426") || name.equals("getNthLine"))
                        && desc.endsWith(")Ljava/lang/String;")) {
                    emitString(mv);
                    return null;
                }
                if ((name.equals("method227") && desc.equals("()Z"))
                        || (name.equals("fpsfoundhere") && desc.equals("(F)Z"))) {
                    emitFalse(mv);
                    return null;
                }
                if (name.equals("method425") && desc.equals("()Ljava/util/Map;")) {
                    emitEmptyMap(mv);
                    return null;
                }
                return mv;
            }

            private void emitInitNoop(MethodVisitor mv) {
                mv.visitCode();
                mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                mv.visitLdcInsn("[C2 patch] Native BASS audio disabled on Apple Silicon");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, "UE_281", "field792", "Z");
                mv.visitInsn(Opcodes.ICONST_M1);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, "UE_281", "field791", "I");
                mv.visitInsn(Opcodes.ICONST_M1);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, "UE_281", "field783", "I");
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "JB_129", "method829", "()F", false);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, "UE_281", "field789", "F");
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            private void emitReturn(MethodVisitor mv) {
                mv.visitCode();
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            private void emitString(MethodVisitor mv) {
                mv.visitCode();
                mv.visitLdcInsn("");
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            private void emitFalse(MethodVisitor mv) {
                mv.visitCode();
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitInsn(Opcodes.IRETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            private void emitEmptyMap(MethodVisitor mv) {
                mv.visitCode();
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "emptyMap", "()Ljava/util/Map;", false);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        };
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        Files.createDirectories(out.getParent());
        Files.write(out, cw.toByteArray());
    }
}
