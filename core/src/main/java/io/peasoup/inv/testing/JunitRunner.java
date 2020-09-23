package io.peasoup.inv.testing;

import io.peasoup.inv.loader.GroovyLoader;
import io.peasoup.inv.run.Logger;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JunitRunner {

    private final JUnitCore junit;
    private final GroovyLoader groovyLoader;
    private final List<Class> classes = new ArrayList<>();

    public JunitRunner() {
        junit = new JUnitCore();
        junit.addListener(new TextListener(System.out));

        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStarImports("org.junit");
        importCustomizer.addStaticStars("org.junit.Assert");

        groovyLoader = new GroovyLoader(false, "io.peasoup.inv.testing.JunitScriptBase", importCustomizer);
    }

    public void add(String scriptLocation) {
        final File scriptFile = new File(scriptLocation);
        if (!scriptFile.exists()) {
            Logger.warn(scriptFile.getAbsolutePath() + " does not exist on current filesystem.");
            return;
        }

        // Load and put class into list
        Class classObj;
        try {
            classObj = groovyLoader.parseClassFile(scriptFile);
        } catch (Exception e) {
            Logger.error(e);
            return;
        }

        classes.add(classObj);
    }

    public boolean run() {
        Result result = junit.run(classes.toArray(new Class[0]));

        System.out.println("Finished. Result: Failures: " + result.getFailureCount() +
                ". Ignored: " + result.getIgnoreCount() +
                ". Tests run: " + result.getRunCount() +
                ". Time: " + result.getRunTime() + "ms.");

        return result.getFailureCount() == 0;
    }


}
