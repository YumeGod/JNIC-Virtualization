import java.io.File;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DLLDumper {
    public static byte[] DumpDLL(String jarPath) {
        invokeJar(jarPath);
        return getLatestDLLBytes();
    }

    public static void invokeJar(String jarPath) {
        System.out.println("Start invoking the static initializer block to decompress the native library");
        try {
            URLClassLoader ucl = (URLClassLoader) DLLDumper.class.getClassLoader();
            ClassLoader loader = new URLClassLoader(new java.net.URL[]{new File(jarPath).toURI().toURL()}, ucl);
            Thread.currentThread().setContextClassLoader(loader);
            Class<?> cls = null;
            cls = Class.forName("dev.jnic." + ProcessLoaderClass.packageName + ".JNICLoader", true, loader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] getLatestDLLBytes() {
        try {
            List<File> list = getFileSort(System.getProperty("java.io.tmpdir"));
            File LatestDLL = list.get(0);
            System.out.println("Target DLL has been successfully found from temp directory, size: " + (int) LatestDLL.length() / (1024 * 1024) + "mb");
            byte[] result = Files.readAllBytes(LatestDLL.toPath());
            LatestDLL.delete();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<File> getFileSort(String path) {
        List<File> list = getFiles(path, new ArrayList<File>());
        System.out.println("Found all JNIC lib files, start comparing");
        if (list != null && list.size() > 0) {

            Collections.sort(list, new Comparator<File>() {
                public int compare(File file, File newFile) {
                    if (file.lastModified() < newFile.lastModified()) {
                        return 1;
                    } else if (file.lastModified() == newFile.lastModified()) {
                        return 0;
                    } else {
                        return -1;
                    }

                }
            });

        }

        return list;
    }

    public static List<File> getFiles(String realpath, List<File> files) {
        System.out.println("Start iterate through temp directory to find JNIC lib files");
        File realFile = new File(realpath);
        if (realFile.isDirectory()) {
            File[] subfiles = realFile.listFiles();
            for (File file : subfiles) {
                if (file.getName().startsWith("lib") && file.getName().endsWith(".tmp")) {
                    files.add(file);
                }
            }
        }
        return files;
    }
}
