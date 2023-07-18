import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DLLDumper {
    public static byte[] DumpDLL(String jarPath) {
        executeJar(jarPath);
        return getLatestDLLBytes();
    }

    public static void executeJar(String jarPath) {
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", jarPath);
        try {
            System.out.println("Jar start executing (Max 30s)");
            Process process = processBuilder.start();
            process.waitFor(30, TimeUnit.SECONDS);
            System.out.println("Jar execution finished");
        } catch (Exception e) {
            System.out.println("An error occurred while executing JAR file: " + e.getMessage());
        }
    }

    public static byte[] getLatestDLLBytes() {
        try {
            List<File> list = getFileSort(System.getProperty("java.io.tmpdir"));
            File LatestDLL = list.get(0);
            System.out.println("target DLL has been successfully found from temp directory");
            return Files.readAllBytes(LatestDLL.toPath());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<File> getFileSort(String path) {
        List<File> list = getFiles(path, new ArrayList<File>());
        System.out.println("Found all JNIC lib file, start comparing");
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
        System.out.println("Start iterate through temp directory to find target DLL");
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
