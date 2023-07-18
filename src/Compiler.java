import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class JavaSourceFromString extends SimpleJavaFileObject {
    final String code;

    public JavaSourceFromString(String name, String code) {
        super(URI.create("string:///" + name.replace('.', '/')
                + Kind.SOURCE.extension), Kind.SOURCE);
        this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }
}

public class Compiler {
    public static void deleteDirectory(Path directory) throws IOException {
        Files.walk(directory)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public static byte[] fileToByteArray(File file) throws IOException {
        FileInputStream fileInputStream = null;
        byte[] byteArray = null;

        try {
            // Create a FileInputStream for the specified file
            fileInputStream = new FileInputStream(file);

            // Create a byte array with the same length as the file
            byteArray = new byte[(int) file.length()];

            // Read the file into the byte array
            fileInputStream.read(byteArray);
        } finally {
            // Close the FileInputStream
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }

        return byteArray;
    }

    public static byte[] compileLoaderClass(String packageName, String DLLName) throws Exception {
        System.out.println("Start compiling custom loader class");

        String compilationPath = "./";

        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        if (jc == null) throw new Exception("Compiler unavailable");

        String code = "package dev.jnic." + packageName + ";import java.io.File;import java.io.InputStream;import java.nio.file.Files;import java.nio.file.StandardCopyOption;public class JNICLoader {    public static void init(){    }    static {        try {            String FileName = \"" + DLLName + "\";            File tempFile = File.createTempFile(\"ProtectedByYumeCloud_\", \".ycvm\");            tempFile.deleteOnExit();            InputStream inputStream = JNICLoader.class.getResourceAsStream(\"/\" + FileName);            if (inputStream != null) {                Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);            } else {                throw new Exception(\"Native library not found!\");            }            System.load(tempFile.getAbsolutePath());        } catch (Exception e) {            e.printStackTrace();        }    }}";
        JavaSourceFromString jsfs = new JavaSourceFromString("JNICLoader", code);

        Iterable<? extends JavaFileObject> fileObjects = Arrays.asList(jsfs);

        List<String> options = new ArrayList<String>();
        options.add("-d");
        options.add(compilationPath);
        options.add("-classpath");
        URLClassLoader urlClassLoader =
                (URLClassLoader) Thread.currentThread().getContextClassLoader();
        StringBuilder sb = new StringBuilder();
        for (URL url : urlClassLoader.getURLs()) {
            sb.append(url.getFile()).append(File.pathSeparator);
        }
        sb.append(compilationPath);
        options.add(sb.toString());

        StringWriter output = new StringWriter();
        boolean success = jc.getTask(output, null, null, options, null, fileObjects).call();
        if (success) {
            System.out.println("Loader class has been successfully compiled");
        } else {
            throw new Exception("Compilation failed :" + output);
        }

        byte[] result = fileToByteArray(new File("./dev/jnic/" + packageName + "/JNICLoader.class"));

        deleteDirectory(Paths.get("dev"));

        return result;
    }
}