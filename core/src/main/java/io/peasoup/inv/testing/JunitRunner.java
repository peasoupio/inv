package io.peasoup.inv.testing;

import io.peasoup.inv.Logger;
import io.peasoup.inv.loader.GroovyLoader;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JunitRunner {

    private static final Map<String, JunitScriptSettings> LISTED_SETTINGS = new HashMap<>(1024);

    public static JunitScriptSettings getMySettings(JunitScriptBase scriptBase) {
        return LISTED_SETTINGS.get(scriptBase.getClass().getName());
    }

    private final String packageName;
    private final JUnitCore junit;
    private final GroovyLoader groovyLoader;
    private final List<Class<?>> classes = new ArrayList<>();

    public JunitRunner(String packageName) {
        if (StringUtils.isEmpty(packageName))
            throw new IllegalArgumentException("packageName");

        this.packageName = packageName;

        junit = new JUnitCore();
        junit.addListener(new InvTextListener());

        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStarImports("org.junit");
        importCustomizer.addStaticStars("org.junit.Assert");

        groovyLoader = GroovyLoader.newBuilder()
            .secureMode(false)
            .scriptBaseClass("io.peasoup.inv.testing.JunitScriptBase")
            .importCustomizer(importCustomizer)
            .build();
    }

    public void addClass(String classLocation) {
        try {
            groovyLoader.addClassFile(new File(classLocation), packageName);
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    public void addTestScript(String scriptLocation) {
        final File scriptFile = new File(scriptLocation);
        if (!scriptFile.exists()) {
            Logger.warn(scriptFile.getAbsolutePath() + " does not exist on current filesystem.");
            return;
        }

        // Load and put class into list
        Class<?> classObj;
        try {
            classObj = groovyLoader.parseTestScriptFile(scriptFile, packageName);
        } catch (Exception e) {
            Logger.error(e);
            return;
        }

        // Register test script settings
        LISTED_SETTINGS.put(classObj.getName(), new JunitScriptSettings());

        // Add classes to the actual Junit runner
        classes.add(classObj);
    }

    public boolean run() {
        if (classes.isEmpty())
            return false;

        Result result = junit.run(classes.toArray(new Class[0]));

        return result.getFailureCount() == 0;
    }

    class JunitScriptSettings {

        protected JunitScriptSettings() {
        }

        public String getPackageName() {
            return packageName;
        }
    }
}
