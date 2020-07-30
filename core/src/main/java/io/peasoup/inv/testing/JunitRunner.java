package io.peasoup.inv.testing;

import groovy.lang.Script;
import io.peasoup.inv.loader.GroovyLoader;
import io.peasoup.inv.run.Logger;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;
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
        junit.addListener(new InvTextListener(System.out));

        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStarImports("org.junit");
        importCustomizer.addStaticStars("org.junit.Assert");

        groovyLoader = new GroovyLoader(false, "io.peasoup.inv.testing.JUnitInvTestingBase", importCustomizer);
    }

    public void add(String scriptLocation) {
        final File scriptFile = new File(scriptLocation);
        if (!scriptFile.exists()) {
            Logger.warn(scriptFile.getAbsolutePath() + " does not exist on current filesystem.");
            return;

        }

        // Load and put class into list
        Script scriptObj;
        try {
            scriptObj = groovyLoader.parseClass(scriptFile);
        } catch (Exception e) {
            Logger.error(e);
            return;
        }

        // Add script location into the metaClass properties
        InvokerHelper.setProperty(
                DefaultGroovyMethods.getMetaClass(scriptObj.getClass()),
                "$0",
                scriptLocation);

        classes.add(scriptObj.getClass());
    }

    public void run() {
        Result result = junit.run(classes.toArray(new Class[0]));
        System.out.println("Finished. Result: Failures: " + result.getFailureCount() +
                ". Ignored: " + result.getIgnoreCount() +
                ". Tests run: " + result.getRunCount() +
                ". Time: " + result.getRunTime() + "ms.");
    }


}
