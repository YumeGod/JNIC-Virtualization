import java.io.File;
import java.util.ArrayList;
import java.util.jar.JarFile;

public class Main {
    public static File file = new File("input.jar");
    public static boolean writeDLL = false;
    public static String ZipComment = "Obfuscation Powered By YumeCloud";
    public static String packageName = null;
    public static String DLLName = null;

    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            if (arg.equalsIgnoreCase("true") || arg.equalsIgnoreCase("false")) {
                writeDLL = Boolean.parseBoolean(arg);
            } else {
                file = new File(arg);
            }
        }

        if (!file.exists()) {
            System.out.println("Input file does not exist!");
            return;
        }

        packageName = ProcessLoaderClass.getPackageName(file);
        System.out.println("JNIC Package Name: " + packageName);

        System.out.println("Start processing native library");
        DLLName = "YCVM/" + ProcessLoaderClass.generateUUID(16) + ".ycvm";
        ProcessLoaderClass.writeDLL(file, DLLName, DLLDumper.DumpDLL(file.getPath()));

        System.out.println("Start processing loader class");
        ArrayList<Integer> verificationParameters = ProcessLoaderClass.findVerificationParameter(new JarFile(file));
        ProcessLoaderClass.deleteLib(file);
        ProcessLoaderClass.writeLoaderClass(file, Compiler.compileLoaderClass(verificationParameters));
    }
}