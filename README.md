<h1 align="center">JNIC Virtualization</h1>
<h4 align="center">a tool that can dump native library from JNIC obfuscated jars and add a custom loader to it</h4>

### Usage

    java -jar JNIC-Virtualization.jar <JNIC Obfuscated Jar> <Dump DLL(true/false)>

### Note

    *** Must use JDK ***
    Be caution with your input jar file, as the application will automatically execute the jar to dump the DLL file.
    Do NOT put any classes/files into dev.jnic.
