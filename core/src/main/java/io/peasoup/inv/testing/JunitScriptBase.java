package io.peasoup.inv.testing;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.Script;
import io.peasoup.inv.Home;
import io.peasoup.inv.MissingOptionException;
import io.peasoup.inv.repo.RepoFolderCollection;
import io.peasoup.inv.repo.RepoURLFetcher;
import io.peasoup.inv.run.InvExecutor;
import io.peasoup.inv.run.InvHandler;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.junit.Before;

import java.io.File;

public abstract class JunitScriptBase extends Script {

    private JunitRunner.JunitScriptSettings mySettings;

    protected RepoFolderCollection repoFolderCollection;
    protected InvExecutor invExecutor;
    protected InvExecutor.Results results;

    private boolean called;

    @Before
    public void setup() {
        mySettings = JunitRunner.getMySettings(this);

        invExecutor = new InvExecutor();
        repoFolderCollection = new RepoFolderCollection(invExecutor);

        called = false;
    }

    public boolean getIsOk() {
        return results != null && results.getReport().isOk();
    }

    public boolean getIsHalted() {
        return results != null && results.getReport().isHalted();
    }

    public boolean getHasExceptions() {
        return results != null  && !results.getReport().getErrors().isEmpty();
    }

    /**
     * Simulate a body using the SimulatorDescriptor to execute tests.
     * @param body The closure body
     * @throws IllegalAccessException Cannot read content.
     * @throws MissingOptionException Cannot parse an INV
     */
    public void simulate(@DelegatesTo(SimulatorDescriptor.class) Closure<?> body) throws IllegalAccessException, MissingOptionException {
        if (body == null)
            throw new IllegalArgumentException("body");

        if (called) throw new IllegalAccessException("Only call sequence once for test method");
        called = true;

        SimulatorDescriptor simulatorDescriptor = new SimulatorDescriptor();

        body.setDelegate(simulatorDescriptor);
        body.setResolveStrategy(Closure.DELEGATE_FIRST);
        body.run();

        if (!simulatorDescriptor.hasSomethingToDo())
            return;

        addResources(simulatorDescriptor);

        // Do the actual execution
        results = invExecutor.execute();
    }

    private void addResources(SimulatorDescriptor simulatorDescriptor) throws MissingOptionException {

        // Add repo files
        for(String repoLocation : simulatorDescriptor.getRepoFiles()) {
            repoFolderCollection.add(repoLocation);
        }

        // Add repo urls
        for(String repoURL : simulatorDescriptor.getRepoUrls()) {
            File localRepoFile = RepoURLFetcher.fetch(repoURL);
            if (localRepoFile == null)
                continue;

            repoFolderCollection.add(localRepoFile.getAbsolutePath());
        }
        repoFolderCollection.bulkRead();

        // Can occur when using "standalone" Groovy executor
        String packageName = null;
        if(mySettings != null)
            packageName = mySettings.getPackageName();

        // Add src files
        File srcFiles = new File(Home.getCurrent(), "src");
        if (simulatorDescriptor.isIncludeSrcFiles() && srcFiles.exists()) {

            File[] files = srcFiles.listFiles();
            if (files != null) {
                for (File srcFile : files) {
                    invExecutor.addClass(srcFile, packageName);
                }
            }
        }

        // Compile classes before executing INv bodies
        invExecutor.compileClasses();

        // Add inv files
        for(String invLocation : simulatorDescriptor.getInvFiles()) {
            File invRealLocation = new File(Home.getCurrent(), invLocation);

            invExecutor.addScript(
                    invRealLocation,
                    packageName,
                    invRealLocation.getParent(),
                    null);
        }

        // Add inv bodies
        for(Closure<?> invBody : simulatorDescriptor.getInvBodies()) {
            new InvHandler(invExecutor).call(invBody);
        }
    }
}
