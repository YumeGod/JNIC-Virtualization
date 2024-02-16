package dev.jnic.*PACKAGE_NAME*;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class JNICLoader {
    public static ByteBuffer z;

    public static void init() {
    }

    static {
        try {
            System.out.println("\n=========================================================\n" + " __     __                    _____ _                 _ \n" +
                    " \\ \\   / /                   / ____| |               | |\n" +
                    "  \\ \\_/ /   _ _ __ ___   ___| |    | | ___  _   _  __| |\n" +
                    "   \\   / | | | '_ ` _ \\ / _ \\ |    | |/ _ \\| | | |/ _` |\n" +
                    "    | || |_| | | | | | |  __/ |____| | (_) | |_| | (_| |\n" +
                    "    |_| \\__,_|_| |_| |_|\\___|\\_____|_|\\___/ \\__,_|\\__,_|\n" + "=========================================================\nObfuscation Powered By YumeCloud\n");
            *VERIFICATION_CODE*
            String nativeLibrary = "*NATIVE_LIBRARY_NAME*";
            File file = File.createTempFile("ProtectedByYumeCloud_", ".ycvm");
            file.deleteOnExit();
            InputStream inputStream = JNICLoader.class.getResourceAsStream("/" + nativeLibrary);
            if (inputStream == null) {
                throw new Exception("Native library not found!");
            }
            Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.load(file.getAbsolutePath());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
