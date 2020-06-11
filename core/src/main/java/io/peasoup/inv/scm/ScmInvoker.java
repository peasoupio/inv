package io.peasoup.inv.scm;

import groovy.lang.Script;
import io.peasoup.inv.run.DebugLogger;
import io.peasoup.inv.run.Logger;
import io.peasoup.inv.security.CommonLoader;

import java.io.File;

public class ScmInvoker {
    private ScmInvoker() {
    }

    public static void invoke(ScmHandler scmHandler, final File scmFile) {
        if (scmHandler == null) {
            throw new IllegalArgumentException("scmHandler");
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
        myNewScript.getBinding().setProperty("scm", scmHandler);
        myNewScript.getBinding().setProperty("debug", DebugLogger.Instance);
        myNewScript.run();
    }

    private static final CommonLoader loader = new CommonLoader();
}
