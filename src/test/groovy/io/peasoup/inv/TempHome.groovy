package io.peasoup.inv

import org.codehaus.plexus.util.FileUtils
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.InitializationError

class TempHome extends BlockJUnit4ClassRunner {
    private static boolean initialized = false

    public static File testResources

    TempHome(Class<?> klass) throws InitializationError {
        super(klass)

        synchronized (TempHome.class) {
            if (initialized)
                return

            // Set temp INV_HOME and clean if already present on filesystem
            Main.currentHome = new File((System.getenv()["TEMP"] ?: '/tmp') + '/inv')
            Main.currentHome.deleteDir()
            Main.currentHome.mkdirs()

            // Move test resources to temp INV_HOME
            def testResourcesSource = new File("./", "src/test/resources")
            testResources = new File(Main.currentHome, "test-resources")
            FileUtils.copyDirectoryStructure(testResourcesSource, testResources)

            initialized = true
        }
    }
}

