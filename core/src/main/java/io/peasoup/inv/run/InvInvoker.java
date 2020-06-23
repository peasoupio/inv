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

    public static void invoke(InvExecutor invExecutor, File scriptPath) throws IOException {
        if (invExecutor == null) {
            throw new IllegalArgumentException("InvExecutor is required");
        }
        if (scriptPath == null) {
            throw new IllegalArgumentException("ScriptPath is required");
        }

        invoke(invExecutor, scriptPath, scriptPath.getAbsoluteFile().getParent(), UNDEFINED_SCM);
    }

    public static void invoke(InvExecutor invExecutor, File scriptFile, String pwd, String scm) throws IOException {
        if (invExecutor == null) {
            throw new IllegalArgumentException("InvExecutor is required");
        }

        if (scriptFile == null) {
            throw new IllegalArgumentException("Script file is required");
        }

        if (!scriptFile.exists()) {
            Logger.warn("[INVOKER] file: " + scriptFile.getCanonicalPath() + ", exists: false");
            return;
        }

        Logger.system("[INVOKER] file: " + scriptFile.getCanonicalPath() + ", exists: true");

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

    private static String checkSubordinateSlash(String path) {
        assert StringUtils.isNotEmpty(path);

        if (path.charAt(path.length() - 1) == '/') return path;

        return path + "/";
    }

}
