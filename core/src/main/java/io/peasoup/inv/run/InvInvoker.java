package io.peasoup.inv.run;

import groovy.lang.Binding;
import groovy.lang.Script;
import io.peasoup.inv.Logger;
import io.peasoup.inv.io.FileUtils;
import io.peasoup.inv.loader.GroovyLoader;
import io.peasoup.inv.loader.YamlLoader;
import io.peasoup.inv.run.yaml.YamlInvHandler;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;

public class InvInvoker {
    public static final String UNDEFINED_REPO = "undefined";

    private final InvExecutor invExecutor;
    private final GroovyLoader groovyLoader;
    private final YamlLoader yamlLoader;

    public InvInvoker(InvExecutor invExecutor) {
        if (invExecutor == null)
            throw new IllegalArgumentException("invExecutor");

        this.invExecutor= invExecutor;
        this.groovyLoader = new GroovyLoader();
        this.yamlLoader = new YamlLoader();
    }

    /**
     * Add a groovy class to the current loader
     *
     * @param classFile   Classfile to load
     * @param packageName New package name assigned to the loaded classes
     */
    public void invokeClass(File classFile, String packageName) {
        if (classFile == null)
            throw new IllegalArgumentException("classFile");

        try {
            groovyLoader.parseClassFile(classFile, packageName);
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    /**
     * Parse and invoke INV Groovy script file
     *
     * @param scriptFile  INV Groovy script file
     */
    public void invokeScript(File scriptFile) {
        invokeScript(scriptFile, null);
    }

    /**
     * Parse and invoke INV Groovy script file
     *
     * @param scriptFile  INV Groovy script file
     * @param packageName  New package assigned to the script file
     */
    public void invokeScript(File scriptFile, String packageName) {
        if (scriptFile == null)
            throw new IllegalArgumentException("ScriptPath is required");

        invokeScript(scriptFile, packageName, scriptFile.getAbsoluteFile().getParent(), UNDEFINED_REPO);
    }

    /**
     * Parse and invoke INV Groovy script file with specific path (pwd).
     * Also, it allows to define the REPO associated to this INV
     *
     * @param scriptFile  INV Groovy script file
     * @param packageName  New package assigned to the script file
     * @param pwd         Pwd "Print working directory", the working directory
     * @param repo        The associated REPO name
     */
    public void invokeScript(File scriptFile, String packageName, String pwd, String repo) {
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
            parseYaml(scriptFile, pwd, repo);
        else
            runScript(scriptFile, packageName, pwd, repo);
    }

    private void parseYaml(File scriptFile, String pwd, String repo) {

        // Create YAML handler
        YamlInvHandler yamlInvHandler = new YamlInvHandler(
                invExecutor,
                yamlLoader,
                scriptFile,
                FileUtils.addSubordinateSlash(pwd),
                StringUtils.isNotEmpty(repo) ? repo : UNDEFINED_REPO);

        try {
            yamlInvHandler.call();
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    private void runScript(File scriptFile, String packageName, String pwd, String repo) {
        Script myNewScript;

        try {
            myNewScript = groovyLoader.parseScriptFile(scriptFile, packageName);
        } catch (Exception e) {
            Logger.error(e);
            return;
        }

        if (myNewScript == null) return;

        InvHandler invHandler = new InvHandler(
                invExecutor,
                scriptFile,
                FileUtils.addSubordinateSlash(pwd),
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


}
