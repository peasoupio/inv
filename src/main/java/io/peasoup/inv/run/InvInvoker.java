package io.peasoup.inv.run;

import groovy.lang.Binding;
import groovy.lang.Script;
import io.peasoup.inv.security.CommonLoader;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class InvInvoker {
    public static final String UNDEFINED_SCM = "undefined";
    private static final CommonLoader loader = new CommonLoader();

    private InvInvoker() {

    }

    public static void invoke(InvHandler invHandler, File scriptPath) throws IOException {
        if (invHandler == null) {
            throw new IllegalArgumentException("InvHandler is required");
        }
        if (scriptPath == null) {
            throw new IllegalArgumentException("ScriptPath is required");
        }

        invoke(invHandler, scriptPath, scriptPath.getAbsoluteFile().getParent(), UNDEFINED_SCM);
    }

    public static void invoke(InvHandler invHandler, File scriptFile, String pwd, String scm) throws IOException {
        if (invHandler == null) {
            throw new IllegalArgumentException("InvHandler is required");
        }

        if (scriptFile == null) {
            throw new IllegalArgumentException("Script file is required");
        }

        if (!scriptFile.exists()) {
            Logger.warn("INV file does not exists: " + scriptFile.getCanonicalPath());
            return;
        }

        Logger.system("[INVOKER] file: " + scriptFile.getCanonicalPath());

        if (StringUtils.isEmpty(pwd)) {
            throw new IllegalArgumentException("Pwd (current working directory) is required");
        }

        String preferredClassname = (normalizeClassName(scriptFile) + "_" + checksum(scriptFile)).toLowerCase();
        Script myNewScript;

        try {
            myNewScript = loader.parseClass(ResourceGroovyMethods.getText(scriptFile), cache(scriptFile, preferredClassname));
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            Logger.error(e);
            return;
        }

        if (myNewScript == null) return;

        Binding binding = myNewScript.getBinding();

        binding.setProperty("inv", invHandler);
        binding.setProperty("$0", scriptFile.getCanonicalPath());
        binding.setProperty("pwd", checkSubordinateSlash(pwd));
        binding.setProperty("scm", StringUtils.isNotEmpty(scm) ? scm : UNDEFINED_SCM);
        binding.setProperty("debug", DebugLogger.Instance);

        try {
            myNewScript.run();
        } catch (Exception ex) {
            Logger.error(ex);
        }

    }

    public static String cache(File scriptFile, final String classname) throws IOException {
        if (scriptFile == null) {
            throw new IllegalArgumentException("Script file is required");
        }
        if (!scriptFile.exists()) {
            throw new IllegalArgumentException("Script file must exists");
        }
        if (StringUtils.isEmpty(classname)) {
            throw new IllegalArgumentException("Classname is required");
        }

        File cache = new File(RunsRoller.getLatest().folder(), "scripts/");

        // Make sure cache is available with minimal accesses
        if (!cache.exists()) {
            Logger.system("Created cache folder: " + cache.mkdirs());

            // https://stackoverflow.com/questions/5302269/java-file-setwritable-and-stopped-working-correctly-after-jdk-6u18
            if (!cache.setExecutable(true)) {
                throw new IllegalArgumentException("Could not set executable");
            }
            
            boolean writabledSet = cache.setWritable(true);
            Logger.system("[SECURITY] writable: " + writabledSet);

            if (!cache.setReadable(true)) {
                throw new IllegalArgumentException("Could not set readable");
            }
        }

        final File filename = new File(cache, classname + ".groovy");
        Logger.system("Created filename folder: " + filename.mkdirs());

        // Make sure we got latest
        Files.delete(Paths.get(filename.getAbsolutePath()));

        // Create a symlink to have dynamic updates adn save space
        //Files.createSymbolicLink(Paths.get(filename.absolutePath), Paths.get(scriptFile.absolutePath))
        Files.copy(Paths.get(scriptFile.getAbsolutePath()), Paths.get(filename.getAbsolutePath()));

        Logger.system("[CACHE] file: " + filename.getAbsolutePath());

        return filename.getAbsolutePath();
    }

    protected static String normalizeClassName(File script) {
        if (script.getParent() == null) return script.getName().split("\\.")[0];

        if (script.getName().equalsIgnoreCase("inv")) return script.getParentFile().getName();

        if (script.getName().equalsIgnoreCase("inv.groovy")) return script.getParentFile().getName();

        return script.getName().split("\\.")[0];
    }

    private static String checksum(File path) throws IOException {

        String checksumValue = path.getName();

        try {
            try (
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)
            ) {
                oos.writeObject(path.getAbsolutePath());
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] thedigest = md.digest(baos.toByteArray());
                checksumValue = DatatypeConverter.printHexBinary(thedigest);
            }
        } catch (NoSuchAlgorithmException e) {
            Logger.error(e);
        }

        return checksumValue;
    }

    protected static String checkSubordinateSlash(String path) {
        assert StringUtils.isNotEmpty(path);

        if (path.charAt(path.length() - 1) == '/') return path;

        return path + "/";
    }
}
