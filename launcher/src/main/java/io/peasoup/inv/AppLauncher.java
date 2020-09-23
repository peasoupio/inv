package io.peasoup.inv;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Launch a new "inv" cli process with minimal required libraries.
 * Using this mechanism prevent Composer's JAR being injected into INV "scripting" libraries.
 * Also, it allows the CLI and Composer to launch using the same entry point.
 */
public class AppLauncher {

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {

        File jarLocation = Paths.get(AppLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile();
        File appFolder = jarLocation.getParentFile();

        // Check lib folder
        File libFolder = new File(appFolder, "lib/");
        if (!libFolder.exists())
            throw new IllegalStateException("./lib folder must exist on the current filesystem.");

        File[] libFolderFiles = libFolder.listFiles();
        if (libFolderFiles == null)
            throw new IllegalStateException("./lib has no valid files");

        // Check libext folder
        File libextFolder = new File(appFolder, "libext/");
        if (!libextFolder.exists())
            throw new IllegalStateException("./libext folder must exist on the current filesystem.");

        File[] libextFolderFiles = libextFolder.listFiles();
        if (libextFolderFiles == null)
            throw new IllegalStateException("./libext has no valid files");

        // Get URLS
        List<String> urls = new ArrayList<>();

        // Get lib URLS by default
        for(File lib : libFolderFiles) {
            if (!lib.getName().endsWith(".jar"))
                continue;

            urls.add(lib.getCanonicalPath());
        }

        // Add libext, only if required
        if (requireExt(args)) {
            // Get lib URLS by default
            for(File lib : libextFolderFiles) {
                if (!lib.getName().endsWith(".jar"))
                    continue;

                urls.add(lib.getCanonicalPath());
            }
        }

        // Determiner classpath delimiter
        String delimiter = ":";
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            delimiter = ";";
        }

        // Create commands
        List<String> commands = new ArrayList<>();
        commands.add("java");
        commands.add("-Djava.system.class.loader=groovy.lang.GroovyClassLoader");
        commands.add("-cp");
        commands.add(String.join(delimiter, urls));
        commands.add("io.peasoup.inv.Main");

        // Parse args
        Collections.addAll(commands, args);

        // Start process
        ProcessBuilder proc = new ProcessBuilder(commands.toArray(new String[0]));
        proc.environment().put("APPLAUNCHER", Paths.get(AppLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
        proc.directory(new File("./"));
        proc.inheritIO();

        proc.start().waitFor();
    }

    /**
     * Determine whether or not a cli option requires the extented librairies.
     * @param args the current args
     * @return True if required, otherwise false
     */
    private static boolean requireExt(String[] args) {
        if (args == null || args.length == 0)
            return false;

        if ("run".equals(args[0]))
            return false;

        if ("scm".equals(args[0]))
            return false;

        if ("synthax".equals(args[0]))
            return false;

        return true;
    }
}
