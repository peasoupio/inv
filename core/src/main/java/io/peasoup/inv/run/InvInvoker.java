package io.peasoup.inv.run;

import groovy.lang.Binding;
import groovy.lang.Script;
import io.peasoup.inv.loader.GroovyLoader;
import io.peasoup.inv.loader.YamlLoader;
import io.peasoup.inv.run.yaml.YamlInvHandler;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;

public class InvInvoker {
    public static final String UNDEFINED_REPO = "undefined";
    private static final GroovyLoader loader = new GroovyLoader();

    private InvInvoker() {
        // empty ctor
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

        invoke(invExecutor, scriptFile, scriptFile.getAbsoluteFile().getParent(), UNDEFINED_REPO);
    }

    /**
     * Parse and invoke INV Groovy script file with specific path (pwd).
     * Also, it allows to define the REPO associated to this INV
     * @param invExecutor Executor instance
     * @param scriptFile INV Groovy script file
     * @param pwd Pwd "Print working directory", the working directory
     * @param repo The associated REPO name
     */
    public static void invoke(InvExecutor invExecutor, File scriptFile, String pwd, String repo) {
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

        if (StringUtils.isEmpty(pwd)) {
            throw new IllegalArgumentException("Pwd (current working directory) is required");
        }

        // Check if either a YAML or Groovy Script file
        if (scriptPath.endsWith(".yaml") || scriptPath.endsWith(".yml"))
            parseYaml(invExecutor, scriptFile,pwd, repo, scriptPath);
        else
            runScript(invExecutor, scriptFile,pwd, repo, scriptPath);
    }

    private static void parseYaml(InvExecutor invExecutor, File scriptFile, String pwd, String repo, String scriptPath) {

        // Create YAML handler
        YamlInvHandler yamlInvHandler = new YamlInvHandler(
                invExecutor,
                scriptPath,
                checkSubordinateSlash(pwd),
                StringUtils.isNotEmpty(repo) ? repo : UNDEFINED_REPO);

        try {
            yamlInvHandler.call(YamlLoader.parseYaml(scriptFile));
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    private static void runScript(InvExecutor invExecutor, File scriptFile, String pwd, String repo, String scriptPath) {
        Script myNewScript;

        try {
            myNewScript = loader.parseScriptFile(scriptFile);
        } catch (Exception e) {
            Logger.error(e);
            return;
        }

        if (myNewScript == null) return;

        InvHandler invHandler = new InvHandler(
                invExecutor,
                scriptPath,
                checkSubordinateSlash(pwd),
                StringUtils.isNotEmpty(repo) ? repo : UNDEFINED_REPO);

        Binding binding = myNewScript.getBinding();
        binding.setProperty("inv", invHandler);
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
