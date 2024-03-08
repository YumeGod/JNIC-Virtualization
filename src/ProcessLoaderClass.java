import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.UUID;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ProcessLoaderClass {
    public static ArrayList<Integer> findVerificationParameter(JarFile file) {
        ArrayList<Integer> result = new ArrayList<>();
        InputStream classFileInputStream = null;

        try {
            classFileInputStream = file.getInputStream(file.getJarEntry("dev/jnic/" + Main.packageName + "/JNICLoader" + ".class"));
            ClassReader classReader = new ClassReader(classFileInputStream);
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, ClassReader.SKIP_DEBUG);

            for (MethodNode methodNode : classNode.methods) {
                if (!methodNode.name.equals("<clinit>")) continue;

                AbstractInsnNode temp = null;
                for (AbstractInsnNode insnNode : methodNode.instructions) {
                    if (insnNode instanceof MethodInsnNode) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                        if (methodInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL && methodInsnNode.owner.equals("java/nio/ByteBuffer") && methodInsnNode.name.equals("putInt")) {
                            System.out.println("Found putInt() call");

                            if (temp instanceof LdcInsnNode) {
                                LdcInsnNode ldcInsnNode = (LdcInsnNode) temp;
                                if (ldcInsnNode.cst instanceof Integer) {
                                    System.out.println("Parameter Integer value: " + ldcInsnNode.cst);
                                    result.add((Integer) ldcInsnNode.cst);
                                }
                            }
                        }
                    }
                    temp = insnNode;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    public static void deleteLib(File target) {
        try {
            ZipFile zip = new ZipFile(target);
            File tempFile = File.createTempFile(zip.getName(), null);

            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile));

            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                String entryName = entry.getName();
                if (!entryName.startsWith("dev/jnic/lib/") && !entryName.startsWith("dev/jnic/" + Main.packageName + "/")) {
                    zos.putNextEntry(new ZipEntry(entryName));
                    InputStream is = zip.getInputStream(entry);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        zos.write(buffer, 0, bytesRead);
                    }
                    is.close();
                    zos.closeEntry();
                } else {
                    System.out.println(entryName + " deleted!");
                }
            }

            zip.close();
            zos.close();

            Files.copy(tempFile.toPath(), new FileOutputStream(target));

            tempFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getPackageName(File target) {
        try {
            ZipFile zip = new ZipFile(target);
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                String fileName = entry.getName();
                if (fileName.startsWith("dev/jnic/") && fileName.endsWith("JNICLoader.class") && fileName.split("/")[2].length() == 6) {
                    return fileName.split("/")[2];
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void writeLoaderClass(File target, byte[] ClassBytes) {
        try {
            File tmpZip = File.createTempFile(target.getName(), null);

            Files.copy(target.toPath(), new FileOutputStream(tmpZip));

            byte[] buffer = new byte[1024];
            ZipInputStream zin = new ZipInputStream(new FileInputStream(tmpZip));
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(target));

            out.putNextEntry(new ZipEntry("dev/jnic/" + Main.packageName + "/JNICLoader.class"));
            out.write(ClassBytes, 0, ClassBytes.length);

            for (ZipEntry ze = zin.getNextEntry(); ze != null; ze = zin.getNextEntry()) {
                out.putNextEntry(ze);
                for (int read = zin.read(buffer); read > -1; read = zin.read(buffer)) {
                    out.write(buffer, 0, read);
                }
                out.closeEntry();
            }

            out.setComment(Main.ZipComment);
            out.close();
            tmpZip.delete();

            System.out.println("Loader class has been successfully written back to jar");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeDLL(File target, String name, byte[] bytes) {
        try {

            File tmpZip = File.createTempFile(target.getName(), null);

            Files.copy(target.toPath(), new FileOutputStream(tmpZip));

            byte[] buffer = new byte[1024];
            ZipInputStream zin = new ZipInputStream(new FileInputStream(tmpZip));
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(target));

            out.putNextEntry(new ZipEntry(name));
            out.write(bytes, 0, bytes.length);

            for (ZipEntry ze = zin.getNextEntry(); ze != null; ze = zin.getNextEntry()) {
                out.putNextEntry(ze);
                for (int read = zin.read(buffer); read > -1; read = zin.read(buffer)) {
                    out.write(buffer, 0, read);
                }
                out.closeEntry();
            }

            out.close();
            tmpZip.delete();

            System.out.println("Native library has been successfully written back to jar");

            if (Main.writeDLL) {
                String DLLName = Main.DLLName.substring(5, Main.DLLName.length()) + ".dll";
                Files.write(new File(DLLName).toPath(), bytes);
                System.out.println("Native library has been successfully saved as " + DLLName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String generateUUID(int length) {
        UUID uuid = UUID.randomUUID();
        long mostSignificantBits = uuid.getMostSignificantBits();
        long leastSignificantBits = uuid.getLeastSignificantBits();
        String uuidString = String.format("%016x%016x", mostSignificantBits, leastSignificantBits);
        return uuidString.substring(0, length);
    }
}