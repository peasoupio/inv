package io.peasoup.inv.testing;

import groovy.lang.Script;
import io.peasoup.inv.run.InvExecutor;
import io.peasoup.inv.run.InvInvoker;
import io.peasoup.inv.run.PoolReport;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;

import java.io.File;
import java.net.URL;

public abstract class JUnitInvTestingBase extends Script {

    protected InvExecutor invExecutor;
    protected PoolReport report;

    private boolean called;

    @Before
    public void setup() {
        invExecutor = new InvExecutor();
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

    public void invoke(String... files) throws IllegalAccessException {
        if (files == null) throw new IllegalArgumentException("files");
        if (files.length == 0) return;

        if (called) throw new IllegalAccessException("Only call sequence once for test method");
        called = true;

        for (String file : files) {
            runInv(file);
        }

        report = invExecutor.execute();
    }

    private void runInv(String value) {
        if (StringUtils.isEmpty(value)) throw new IllegalArgumentException("Inv must be a valid non-null, non-empty value");

        File invFile = new File(value);

        if (!invFile.exists()) {
            String testClassLocation = (String)getMetaClass().getProperty(this, "$0");
            invFile = new File(new File(testClassLocation).getParentFile(), value);
        }

        if (!invFile.exists()) {
            URL location = this.getClass().getResource(value);
            if (location != null)
                invFile = new File(location.getPath());
        }

        if (!invFile.exists())
            throw new IllegalStateException(invFile.getAbsolutePath() + " does not exists on the filesystem");

        InvInvoker.invoke(invExecutor, invFile);
    }
}
