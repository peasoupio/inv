package io.peasoup.inv.repo;

import groovy.lang.Script;
import io.peasoup.inv.loader.GroovyLoader;
import io.peasoup.inv.loader.YamlLoader;
import io.peasoup.inv.repo.yaml.YamlRepoHandler;
import io.peasoup.inv.run.DebugLogger;
import io.peasoup.inv.run.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RepoInvoker {

    public static final List<String> DEFAULT_EXCLUDED = Collections.unmodifiableList(Arrays.asList(".runs/*", "*.json" ));

    private static GroovyLoader groovyLoader;
    private static YamlLoader yamlLoader;

    private RepoInvoker() {
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
     * Parse and invoke an Repo groovy File
     * @param repoExecutor RepoExecutor instance
     * @param repoFile Repo Groovy file
     */
    public static void invoke(RepoExecutor repoExecutor, File repoFile) {
        invoke(repoExecutor, repoFile, null);
    }

    /**
     * Parse and invoke an REPO groovy File
     * @param repoExecutor RepoExecutor instance
     * @param scriptFile REPO Groovy file
     * @param parametersFile Parameters file to load with the REPO
     */
    public static void invoke(RepoExecutor repoExecutor, File scriptFile, File parametersFile) {
        if (groovyLoader == null || yamlLoader == null)
            throw new IllegalStateException("loader has no cache");

        if (repoExecutor == null) {
            throw new IllegalArgumentException("repoExecutor");
        }
        if (scriptFile == null) {
            throw new IllegalArgumentException("scriptFile");
        }

        if (!scriptFile.exists()) {
            Logger.warn("REPO file does not exists: " + scriptFile.getAbsolutePath());
            return;
        }

        String scriptPath;

        try {
            scriptPath = scriptFile.getCanonicalPath();
        } catch (IOException ex) {
            Logger.warn("[INVOKER] file: " + scriptFile.getAbsolutePath() + ", canonical: false");
            scriptPath = scriptFile.getAbsolutePath();
        }

        // Check if either a YAML or Groovy Script file
        if (scriptPath.endsWith(".yaml") || scriptPath.endsWith(".yml"))
            parseYaml(repoExecutor, scriptFile, parametersFile);
        else
            runScript(repoExecutor, scriptFile, parametersFile);
    }

    private static void parseYaml(RepoExecutor repoExecutor, File scriptFile, File parametersFile) {

        // Create YAML handler
        YamlRepoHandler yamlInvHandler = new YamlRepoHandler(
                repoExecutor,
                yamlLoader,
                scriptFile,
                parametersFile);

        try {
            yamlInvHandler.call();
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    /**
     * Parse and invoke an REPO groovy File
     * @param repoExecutor RepoExecutor instance
     * @param repoFile Repo Groovy file
     * @param parametersFile Parameters file to load with the REPO
     */
    public static void runScript(RepoExecutor repoExecutor, File repoFile, File parametersFile) {
        if (repoExecutor == null) {
            throw new IllegalArgumentException("repoExecutor");
        }
        if (repoFile == null) {
            throw new IllegalArgumentException("repoFile");
        }

        if (!repoFile.exists()) {
            Logger.warn("REPO file does not exists: " + repoFile.getAbsolutePath());
            return;
        }

        // Parse new class
        Script myNewScript = null;
        try {
            myNewScript = groovyLoader.parseScriptFile(repoFile);
        } catch (Exception ex) {
            Logger.error(ex);
        }
        if (myNewScript == null)
            return;

        RepoHandler repoHandler = new RepoHandler(
                repoExecutor,
                repoFile,
                parametersFile);

        // Run new script
        myNewScript.getBinding().setProperty("repo", repoHandler);
        myNewScript.getBinding().setProperty("scm", repoHandler); // @Deprecated
        myNewScript.getBinding().setProperty("debug", DebugLogger.Instance);
        myNewScript.run();
    }


    /**
     * Gets the expected location of a parameters JSON file for a specific repoFile.
     * @param repoFile RepoFile location
     * @return Expected parameters file location
     */
    public static File expectedParametersfileLocation(File repoFile) {
        if (repoFile == null) throw new IllegalArgumentException("repoFile");

        String simpleName = repoFile.getName().split("\\.")[0];
        return new File(repoFile.getParentFile(), simpleName + "-values.json");
    }
}
