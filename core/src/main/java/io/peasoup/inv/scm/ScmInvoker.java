package io.peasoup.inv.scm;

import groovy.lang.Script;
import io.peasoup.inv.run.DebugLogger;
import io.peasoup.inv.run.Logger;
import io.peasoup.inv.loader.GroovyLoader;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ScmInvoker {

    public static final List<String> DEFAULT_EXCLUDED = Collections.unmodifiableList(Arrays.asList(".runs/*", "*.json" ));

    private static final GroovyLoader loader = new GroovyLoader();

    private ScmInvoker() {
    }

    /**
     * Parse and invoke an SCM groovy File
     * @param scmExecutor ScmExecutor instance
     * @param scmFile Scm Groovy file
     */
    public static void invoke(ScmExecutor scmExecutor, File scmFile) {
        invoke(scmExecutor, scmFile, null);
    }

    /**
     * Parse and invoke an SCM groovy File
     * @param scmExecutor ScmExecutor instance
     * @param scmFile Scm Groovy file
     * @param parametersFile Parameters file to load with the SCM
     */
    public static void invoke(ScmExecutor scmExecutor, File scmFile, File parametersFile) {
        if (scmExecutor == null) {
            throw new IllegalArgumentException("scmExecutor");
        }
        if (scmFile == null) {
            throw new IllegalArgumentException("scmFile");
        }

        if (!scmFile.exists()) {
            Logger.warn("SCM file does not exists: " + scmFile.getAbsolutePath());
            return;
        }

        // Parse new class
        Script myNewScript = null;
        try {
            myNewScript = loader.parseClass(scmFile);
        } catch (Exception ex) {
            Logger.error(ex);
        }
        if (myNewScript == null)
            return;

        // Run new script
        myNewScript.getBinding().setProperty("scm", new ScmHandler(scmExecutor, parametersFile));
        myNewScript.getBinding().setProperty("debug", DebugLogger.Instance);
        myNewScript.run();
    }


    /**
     * Gets the expected location of a parameters JSON file for a specific scmFile.
     * @param scmFile ScmFile location
     * @return Expected parameters file location
     */
    public static File expectedParametersfileLocation(File scmFile) {
        if (scmFile == null) throw new IllegalArgumentException("scmFile");

        String simpleName = scmFile.getName().split("\\.")[0];
        return new File(scmFile.getParentFile(), simpleName + ".json");
    }
}
