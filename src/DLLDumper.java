import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class DLLDumper {
    public static CountDownLatch count = new CountDownLatch(1);

    public static byte[] DumpDLL(String jarPath) {
        try {
            getNativeLibrary(jarPath);
            invokeJar(jarPath);
            count.await();
            return Files.readAllBytes(new File(System.getProperty("user.dir") + "\\jnic_nativeLibrary").toPath());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void invokeJar(String jarPath) {
        new Thread(() -> {
            try {
                URLClassLoader ucl = (URLClassLoader) DLLDumper.class.getClassLoader();
                ClassLoader loader = new URLClassLoader(new URL[]{new File(jarPath).toURI().toURL()}, ucl);
                Thread.currentThread().setContextClassLoader(loader);
                Class<?> cls = null;
                cls = Class.forName("dev.jnic." + Main.packageName + ".JNICLoader", true, loader);
            } catch (Exception e) {
                e.printStackTrace();
            }
            count.countDown();
        }).start();
    }

    public static void getNativeLibrary(String jarPath) {
        InputStream classFileInputStream = null;
        JarFile file = null;

        try {
            file = new JarFile(jarPath);

            classFileInputStream = file.getInputStream(file.getJarEntry("dev/jnic/" + Main.packageName + "/JNICLoader" + ".class"));

            ClassReader classReader = new ClassReader(classFileInputStream);
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, 0);

            for (MethodNode methodNode : classNode.methods) {
                if (!methodNode.name.equals("<clinit>")) continue;

                for (AbstractInsnNode insnNode : methodNode.instructions) {
                    if (insnNode instanceof MethodInsnNode) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                        if (methodInsnNode.getOpcode() == Opcodes.INVOKESTATIC && methodInsnNode.owner.equals("java/io/File") && methodInsnNode.name.equals("createTempFile")) {
                            System.out.println("Found createTempFile() call, replacing it with new File() at custom directory");

                            InsnList newInstructions = new InsnList();
                            newInstructions.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
                            newInstructions.add(new InsnNode(Opcodes.DUP));
                            newInstructions.add(new LdcInsnNode(System.getProperty("user.dir") + "\\jnic_nativeLibrary"));
                            newInstructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V", false));

                            methodNode.instructions.insert(insnNode, newInstructions);
                            methodNode.instructions.remove(insnNode.getPrevious().getPrevious());
                            methodNode.instructions.remove(insnNode.getPrevious());
                            methodNode.instructions.remove(insnNode);
                        } else if (methodInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL && methodInsnNode.owner.equals("java/io/File") && methodInsnNode.name.equals("exists") && methodInsnNode.getNext().getOpcode() == Opcodes.IFNE) {
                            InsnList newInstructions = new InsnList();
                            newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 6));
                            newInstructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "createNewFile", "()Z", false));
                            newInstructions.add(new InsnNode(Opcodes.POP));

                            methodNode.instructions.insert(insnNode.getPrevious().getPrevious(), newInstructions);
                        } else if (methodInsnNode.getOpcode() == Opcodes.INVOKESTATIC && methodInsnNode.owner.equals("java/lang/System") && methodInsnNode.name.equals("load")) {
                            methodNode.instructions.remove(insnNode.getPrevious().getPrevious());
                            methodNode.instructions.remove(insnNode.getPrevious());
                            methodNode.instructions.remove(insnNode);
                        }
                    }
                }
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            classNode.accept(writer);

            byte[] bytes = writer.toByteArray();
            classFileInputStream.close();

            addFileToZip("dev/jnic/" + Main.packageName + "/JNICLoader" + ".class", bytes, new File(jarPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addFileToZip(String sourceFileName, byte[] sourceFile, File zipFile) throws IOException {
        // 创建临时文件来存储更新后的内容
        File tempFile = new File(zipFile.getAbsolutePath() + ".tmp");
        Files.write(tempFile.toPath(), Files.readAllBytes(zipFile.toPath()));

        byte[] buf = new byte[1024];

        ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));

        ZipEntry entry = zin.getNextEntry();
        // 复制原始zip文件中的所有条目到新文件，除了要被替换的文件
        while (entry != null) {
            String name = entry.getName();
            if (!name.equals(sourceFileName)) {
                // 添加ZIP条目
                out.putNextEntry(new ZipEntry(name));
                // 转移数据
                int len;
                while ((len = zin.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            entry = zin.getNextEntry();
        }
        // 关闭输入流
        zin.close();
        // 添加新文件到ZIP
        out.putNextEntry(new ZipEntry(sourceFileName));
        out.write(sourceFile);
        // 关闭流
        out.closeEntry();
        // 完成ZIP文件更新
        out.close();
        tempFile.delete();
    }
}