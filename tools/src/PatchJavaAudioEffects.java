import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class PatchJavaAudioEffects {
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
                    emitInit(mv);
                    return null;
                }
                if (name.equals("method422") && desc.equals("(Lzg_1112;F)V")) {
                    emitPlay(mv);
                    return null;
                }
                if (name.equals("method14") && desc.equals("(I)V")) {
                    emitPlayMusicOffset(mv);
                    return null;
                }
                if (name.equals("method61") && desc.equals("()V")) {
                    emitPlayMenuMusic(mv);
                    return null;
                }
                if (name.equals("method89") && desc.equals("()V")) {
                    emitEnsureMusic(mv);
                    return null;
                }
                if (name.equals("method56") && desc.equals("()V")) {
                    emitShutdown(mv);
                    return null;
                }
                if ((name.equals("method423") && desc.equals("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"))
                        || (name.equals("method424") && desc.equals("(Lzg_1112;Ljava/lang/String;LVf_301;F)V"))
                        || (name.equals("readoggfileslist") && desc.equals("()V"))) {
                    emitReturn(mv);
                    return null;
                }
                if (name.equals("method421") && desc.equals("()Ljava/lang/String;")) {
                    emitMusicString(mv, "currentUrl");
                    return null;
                }
                if (name.equals("method356") && desc.equals("()Ljava/lang/String;")) {
                    emitMusicString(mv, "currentArtist");
                    return null;
                }
                if (name.equals("method426") && desc.equals("()Ljava/lang/String;")) {
                    emitMusicString(mv, "currentTitle");
                    return null;
                }
                if ((name.equals("method420") || name.equals("getNthLine"))
                        && desc.endsWith(")Ljava/lang/String;")) {
                    emitString(mv);
                    return null;
                }
                if (name.equals("fpsfoundhere") && desc.equals("(F)Z")) {
                    emitMusicUpdateFalse(mv);
                    return null;
                }
                if (name.equals("method227") && desc.equals("()Z")) {
                    emitFalse(mv);
                    return null;
                }
                if (name.equals("method425") && desc.equals("()Ljava/util/Map;")) {
                    emitMusicCatalog(mv);
                    return null;
                }
                return mv;
            }

            private void emitInit(MethodVisitor mv) {
                mv.visitCode();
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, "UE_281", "field792", "Z");
                mv.visitInsn(Opcodes.ICONST_M1);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, "UE_281", "field791", "I");
                mv.visitInsn(Opcodes.ICONST_M1);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, "UE_281", "field783", "I");
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "JB_129", "method829", "()F", false);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, "UE_281", "field789", "F");
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "C2JavaAudioEffects", "init", "()V", false);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "C2BassMusic", "init", "()V", false);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            private void emitPlay(MethodVisitor mv) {
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.FLOAD, 1);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "C2JavaAudioEffects", "play", "(Ljava/lang/Object;F)V", false);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            private void emitPlayMusicOffset(MethodVisitor mv) {
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ILOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "C2BassMusic", "playByOffset", "(I)V", false);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            private void emitPlayMenuMusic(MethodVisitor mv) {
                mv.visitCode();
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "C2BassMusic", "playMenuMusic", "()V", false);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            private void emitEnsureMusic(MethodVisitor mv) {
                mv.visitCode();
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "C2BassMusic", "ensurePlaying", "()V", false);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            private void emitShutdown(MethodVisitor mv) {
                mv.visitCode();
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "C2JavaAudioEffects", "shutdown", "()V", false);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "C2BassMusic", "shutdown", "()V", false);
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

            private void emitMusicUpdateFalse(MethodVisitor mv) {
                mv.visitCode();
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "C2BassMusic", "update", "()V", false);
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitInsn(Opcodes.IRETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            private void emitMusicString(MethodVisitor mv, String methodName) {
                mv.visitCode();
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "C2BassMusic", methodName, "()Ljava/lang/String;", false);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            private void emitMusicCatalog(MethodVisitor mv) {
                mv.visitCode();
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "C2BassMusic", "catalog", "()Ljava/util/Map;", false);
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
