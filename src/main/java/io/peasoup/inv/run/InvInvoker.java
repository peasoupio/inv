package io.peasoup.inv.run;

import groovy.lang.Script;
import io.peasoup.inv.security.CommonLoader;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.codehaus.groovy.runtime.StackTraceUtils;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class InvInvoker {
    public static final String UNDEFINED_SCM = "undefined";
    private static final CommonLoader loader = new CommonLoader();

    public static void invoke(InvHandler invHandler, File scriptPath) throws IOException {
        assert invHandler != null : "InvHandler is required";
        assert scriptPath != null : "ScriptPath is required";

        invoke(invHandler, scriptPath.getParent(), scriptPath, UNDEFINED_SCM);
    }

    public static void invoke(InvHandler invHandler, String pwd, final File scriptFile, String scm) throws IOException {
        assert invHandler != null : "InvHandler is required";
        assert StringUtils.isNotEmpty(pwd) : "Pwd (current working directory) is required";
        assert scriptFile != null : "Script file is required";

        if (!scriptFile.exists()) {
            Logger.warn("INV file does not exists: " + scriptFile.getCanonicalPath());
            return;
        }


        Logger.debug("file: " + scriptFile.getCanonicalPath());

        String preferredClassname = (normalizeClassName(scriptFile) + "_" + checksum(scriptFile)).toLowerCase();
        Script myNewScript;

        try {
            myNewScript = loader.parseClass(ResourceGroovyMethods.getText(scriptFile), cache(scriptFile, preferredClassname));
        } catch (IllegalAccessException e) {
            Logger.error(StackTraceUtils.sanitize(e));
            return;
        } catch (InstantiationException e) {
            Logger.error(StackTraceUtils.sanitize(e));
            return;
        }

        if (myNewScript == null) return;

        myNewScript.getBinding().setProperty("inv", invHandler);
        myNewScript.getBinding().setProperty("$0", scriptFile.getCanonicalPath());
        myNewScript.getBinding().setProperty("pwd", checkSubordinateSlash(pwd));
        myNewScript.getBinding().setProperty("scm", StringUtils.isNotEmpty(scm) ? scm : UNDEFINED_SCM);

        try {
            myNewScript.run();
        } catch (Exception ex) {
            Logger.error(StackTraceUtils.sanitize(ex));
        }

    }

    public static String cache(File scriptFile, final String classname) throws IOException {
        assert scriptFile != null : "Script file is required";
        assert scriptFile.exists() : "Script file must exists";
        assert StringUtils.isNotEmpty(classname) : "Classname is required";

        File cache = new File(RunsRoller.getLatest().folder(), "scripts/");

        // Make sure cache is available with minimal accesses
        if (!cache.exists()) {
            cache.mkdir();
            cache.setExecutable(true);
            cache.setWritable(true);
            cache.setReadable(true);
        }


        final File filename = new File(cache, classname + ".groovy");
        filename.mkdirs();

        // Make sure we got latest
        Files.delete(Paths.get(filename.getAbsolutePath()));

        // Create a symlink to have dynamic updates adn save space
        //Files.createSymbolicLink(Paths.get(filename.absolutePath), Paths.get(scriptFile.absolutePath))
        Files.copy(Paths.get(scriptFile.getAbsolutePath()), Paths.get(filename.getAbsolutePath()));

        Logger.debug("created copy for " + classname + " here: " + filename.getAbsolutePath());

        return filename.getAbsolutePath();
    }

    protected static String normalizeClassName(File script) {
        if (script.getParent() == null) return script.getName().split("\\.")[0];

        if (script.getName().toLowerCase().equals("inv")) return script.getParentFile().getName();

        if (script.getName().toLowerCase().equals("inv.groovy")) return script.getParentFile().getName();

        return script.getName().split("\\.")[0];
    }

    private static String checksum(File path) throws IOException {
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        String checksumValue = path.getName();

        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(path.getAbsolutePath());
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] thedigest = md.digest(baos.toByteArray());
            checksumValue = DatatypeConverter.printHexBinary(thedigest);
        } catch (NoSuchAlgorithmException e) {
            Logger.error(e);
        } finally {
            oos.close();
            baos.close();
        }

        return checksumValue;
    }

    protected static String checkSubordinateSlash(String path) {
        assert StringUtils.isNotEmpty(path);

        if (path.charAt(path.length() - 1) == '/') return path;

        return path + "/";
    }
}
