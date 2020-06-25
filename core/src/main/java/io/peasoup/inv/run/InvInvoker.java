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

    /**
     * Parse and invoke INV Groovy script file
     * @param invExecutor Executor instance
     * @param scriptFile INV Groovy script file
     */
    public static void invoke(InvExecutor invExecutor, File scriptFile) {
        if (invExecutor == null) {
            throw new IllegalArgumentException("InvExecutor is required");
        }
        if (scriptFile == null) {
            throw new IllegalArgumentException("ScriptPath is required");
        }

        invoke(invExecutor, scriptFile, scriptFile.getAbsoluteFile().getParent(), UNDEFINED_SCM);
    }

    /**
     * Parse and invoke INV Groovy script file with specific path (pwd).
     * Also, it allows to define the SCM associated to this INV
     * @param invExecutor Executor instance
     * @param scriptFile INV Groovy script file
     * @param pwd Pwd "Print working directory", the working directory
     * @param scm The associated SCM name
     */
    public static void invoke(InvExecutor invExecutor, File scriptFile, String pwd, String scm) {
        if (invExecutor == null) {
            throw new IllegalArgumentException("InvExecutor is required");
        }

        if (scriptFile == null) {
            throw new IllegalArgumentException("Script file is required");
        }

        String scriptPath;

        try {
            scriptPath = scriptFile.getCanonicalPath();
        } catch (IOException ex) {
            Logger.warn("[INVOKER] file: " + scriptFile.getAbsolutePath() + ", canonical: false");
            scriptPath = scriptFile.getAbsolutePath();
        }

        if (!scriptFile.exists()) {
            Logger.warn("[INVOKER] file: " + scriptPath + ", exists: false");
            return;
        }

        //Logger.system("[INVOKER] file: " + scriptPath + ", exists: true");

        if (StringUtils.isEmpty(pwd)) {
            throw new IllegalArgumentException("Pwd (current working directory) is required");
        }

        Script myNewScript;

        try {
            myNewScript = loader.parseClass(scriptFile);
        } catch (Exception e) {
            Logger.error(e);
            return;
        }

        if (myNewScript == null) return;

        Binding binding = myNewScript.getBinding();

        binding.setProperty("inv", new InvHandler(invExecutor));
        binding.setProperty("$0", scriptPath);
        binding.setProperty("pwd", checkSubordinateSlash(pwd));
        binding.setProperty("scm", StringUtils.isNotEmpty(scm) ? scm : UNDEFINED_SCM);
        binding.setProperty("debug", DebugLogger.Instance);

        try {
            myNewScript.run();
        } catch (Exception ex) {
            Logger.error(ex);
        }

    }

    private static String checkSubordinateSlash(String path) {
        assert StringUtils.isNotEmpty(path);

        if (path.charAt(path.length() - 1) == '/') return path;

        return path + "/";
    }

}
