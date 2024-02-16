import java.io.File;
import java.util.ArrayList;
import java.util.jar.JarFile;

public class Main {
    public static boolean writeDLL = false;
    public static String DLLName = null;

    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);
        writeDLL = Boolean.parseBoolean(args[1]);

        ProcessLoaderClass.packageName = ProcessLoaderClass.getPackageName(file);
        System.out.println("PackageName: " + ProcessLoaderClass.packageName);

        System.out.println("Start processing native library");
        DLLName = "YCVM/" + ProcessLoaderClass.generateUUID(16) + ".ycvm";
        ProcessLoaderClass.writeDLL(file, DLLName, DLLDumper.DumpDLL(file.getPath()));

        System.out.println("Start processing loader class");
        ArrayList<Integer> verificationParameters = ProcessLoaderClass.findVerificationParameter(new JarFile(file));
        ProcessLoaderClass.deleteLib(file);
        ProcessLoaderClass.writeLoaderClass(file, Compiler.compileLoaderClass(verificationParameters));
    }
}
