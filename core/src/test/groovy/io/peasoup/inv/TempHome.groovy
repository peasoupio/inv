package io.peasoup.inv

import io.peasoup.inv.run.RunsRoller
import org.codehaus.plexus.util.FileUtils
import org.junit.runner.notification.RunNotifier
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.InitializationError

class TempHome extends BlockJUnit4ClassRunner {

    public static File testResources

    TempHome(Class<?> klass) throws InitializationError {
        super(klass)
    }

    @Override
    void run(RunNotifier notifier) {
        // Set temp INV_HOME and clean if already present on filesystem
        Home.setCurrent(new File((System.getenv()["TEMP"] ?: '/tmp') + '/inv'))
        Home.getCurrent().deleteDir()
        Home.getCurrent().mkdirs()

        // Clear existing tests runs
        RunsRoller.forceDelete()

        // Move test resources to temp INV_HOME
        def testResourcesSource = new File("./", "src/test/resources")
        testResources = new File(Home.getCurrent(), "test-resources")
        FileUtils.copyDirectoryStructure(testResourcesSource, testResources)

        // Call getLatest to make sure /runs exists
        RunsRoller.getLatest()

        super.run(notifier)
    }
}

