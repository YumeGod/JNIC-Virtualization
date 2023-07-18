import java.io.File;

public class Main {
    public static boolean writeDLL = false;
    public static String DLLName = null;

    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);
        writeDLL = Boolean.parseBoolean(args[1]);

        System.out.println("Start processing native library");
        DLLName = "YCVM/" + ProcessLoaderClass.generateUUID(16) + ".ycvm";
        System.out.println("DLLName: " + DLLName);
        ProcessLoaderClass.writeDLL(file, DLLName, DLLDumper.DumpDLL(file.getPath()));

        System.out.println("Start processing loader class");
        ProcessLoaderClass.packageName = ProcessLoaderClass.getPackageName(file);
        System.out.println("PackageName: " + ProcessLoaderClass.packageName);
        ProcessLoaderClass.deleteLib(file);
        ProcessLoaderClass.writeLoaderClass(file, Compiler.compileLoaderClass(ProcessLoaderClass.packageName, DLLName));
    }
}
