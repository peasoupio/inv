package io.peasoup.inv.testing;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.Script;
import io.peasoup.inv.Home;
import io.peasoup.inv.MissingOptionException;
import io.peasoup.inv.repo.RepoFolderCollection;
import io.peasoup.inv.repo.RepoURLExtractor;
import io.peasoup.inv.run.InvExecutor;
import io.peasoup.inv.run.InvHandler;
import io.peasoup.inv.run.PoolReport;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;

import java.io.File;
import java.net.URL;

public abstract class JunitScriptBase extends Script {

    private JunitRunner.JunitScriptSettings mySettings;

    protected RepoFolderCollection repoFolderCollection;
    protected InvExecutor invExecutor;
    protected PoolReport report;

    private boolean called;

    @Before
    public void setup() {
        mySettings = JunitRunner.getMySettings(this);

        invExecutor = new InvExecutor();
        repoFolderCollection = new RepoFolderCollection(invExecutor);

        called = false;
    }

    public boolean getIsOk() {
        return report != null && report.isOk();
    }

    public boolean getIsHalted() {
        return report != null && report.isHalted();
    }

    public boolean getHasExceptions() {
        return report != null  && !report.getErrors().isEmpty();
    }

    public void simulate(@DelegatesTo(SimulatorDescriptor.class) Closure body) throws IllegalAccessException, MissingOptionException {
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

        // Add repo files
        for(String repoLocation : simulatorDescriptor.getRepoFiles()) {
            repoFolderCollection.add(repoLocation);
        }

        // Add repo urls
        for(String repoURL : simulatorDescriptor.getRepoUrls()) {
            File localRepoFile = RepoURLExtractor.extract(repoURL);
            if (localRepoFile == null)
                continue;

            repoFolderCollection.add(localRepoFile.getAbsolutePath());
        }

        repoFolderCollection.loadInvs();

        // Add inv files
        for(String invLocation : simulatorDescriptor.getInvFiles()) {
            File invRealLocation = new File(Home.getCurrent(), invLocation);

            invExecutor.addScript(
                    invRealLocation,
                    mySettings.getPackageName(),
                    invRealLocation.getParent(),
                    null);
        }

        // Add inv bodies
        for(Closure invBody : simulatorDescriptor.getInvBodies()) {
            new InvHandler(invExecutor).call(invBody);
        }

        // Do the actual execution
        report = invExecutor.execute();
    }

    private void loadInvScriptfile(String invScriptfile) {
        if (StringUtils.isEmpty(invScriptfile)) throw new IllegalArgumentException("Inv must be a valid non-null, non-empty value");

        File invFile = new File(Home.getCurrent(), invScriptfile);

        if (!invFile.exists()) {
            String testClassLocation = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            invFile = new File(new File(testClassLocation).getParentFile(), invScriptfile);
        }

        if (!invFile.exists()) {
            URL location = this.getClass().getResource(invScriptfile);
            if (location != null)
                invFile = new File(location.getPath());
        }

        if (!invFile.exists())
            throw new IllegalStateException(invFile.getAbsolutePath() + " does not exists on the filesystem");

        invExecutor.addScript(invFile, mySettings.getPackageName());
    }
}
