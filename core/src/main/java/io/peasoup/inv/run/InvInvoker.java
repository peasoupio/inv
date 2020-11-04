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
    private static GroovyLoader groovyLoader;
    private static YamlLoader yamlLoader;

    private InvInvoker() {
        // empty ctor
    }

    /**
     * Clear existing loader and create a new instance.
     */
    public static void newCache() {
        groovyLoader = new GroovyLoader();
        yamlLoader = new YamlLoader();
    }

    /**
     * Add a groovy class to the current loader
     * @param classFile Classfile to load
     * @param packageName New package name assigned to the loaded classes
     */
    public static void addClass(File classFile, String packageName) {
        if (groovyLoader == null || yamlLoader == null)
            throw new IllegalStateException("loader has no cache");

        try {
            groovyLoader.parseClassFile(classFile, packageName);
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    /**
     * Parse and invoke INV Groovy script file
     * @param invExecutor Executor instance
     * @param scriptFile INV Groovy script file
     */
    public static void invoke(InvExecutor invExecutor, File scriptFile) {
        invoke(invExecutor, scriptFile, null);
    }

    /**
     * Parse and invoke INV Groovy script file
     * @param invExecutor Executor instance
     * @param scriptFile INV Groovy script file
     * @param newPackage New package assigned to the script file
     */
    public static void invoke(InvExecutor invExecutor, File scriptFile, String newPackage) {
        if (invExecutor == null) {
            throw new IllegalArgumentException("InvExecutor is required");
        }
        if (scriptFile == null) {
            throw new IllegalArgumentException("ScriptPath is required");
        }

        invoke(invExecutor, scriptFile, newPackage, scriptFile.getAbsoluteFile().getParent(), UNDEFINED_REPO);
    }

    /**
     * Parse and invoke INV Groovy script file with specific path (pwd).
     * Also, it allows to define the REPO associated to this INV
     * @param invExecutor Executor instance
     * @param scriptFile INV Groovy script file
     * @param newPackage New package assigned to the script file
     * @param pwd Pwd "Print working directory", the working directory
     * @param repo The associated REPO name
     */
    public static void invoke(InvExecutor invExecutor, File scriptFile, String newPackage, String pwd, String repo) {
        if (groovyLoader == null || yamlLoader == null)
            throw new IllegalStateException("loader has no cache");

        if (invExecutor == null)
            throw new IllegalArgumentException("InvExecutor is required");

        if (scriptFile == null)
            throw new IllegalArgumentException("Script file is required");

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
            parseYaml(invExecutor, scriptFile,pwd, repo);
        else
            runScript(invExecutor, scriptFile, newPackage, pwd, repo, scriptPath);
    }

    private static void parseYaml(InvExecutor invExecutor, File scriptFile, String pwd, String repo) {

        // Create YAML handler
        YamlInvHandler yamlInvHandler = new YamlInvHandler(
                invExecutor,
                yamlLoader,
                scriptFile,
                checkSubordinateSlash(pwd),
                StringUtils.isNotEmpty(repo) ? repo : UNDEFINED_REPO);

        try {
            yamlInvHandler.call();
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    private static void runScript(InvExecutor invExecutor, File scriptFile, String newPackage, String pwd, String repo, String scriptPath) {
        Script myNewScript;

        try {
            myNewScript = groovyLoader.parseScriptFile(scriptFile, newPackage);
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
