package io.peasoup.inv.testing;

import groovy.lang.Closure;
import groovy.lang.Script;
import io.peasoup.inv.run.InvExecutor;
import io.peasoup.inv.run.InvHandler;
import io.peasoup.inv.run.InvInvoker;
import io.peasoup.inv.run.PoolReport;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;

import java.io.File;
import java.net.URL;

public abstract class JunitScriptBase extends Script {

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

    public void simulate(Object... invs) throws IllegalAccessException, InvHandler.INVOptionRequiredException {
        if (invs == null) throw new IllegalArgumentException("invs");
        if (invs.length == 0) return;

        if (called) throw new IllegalAccessException("Only call sequence once for test method");
        called = true;

        for (Object inv : invs) {
            if (inv instanceof CharSequence) loadInvScriptfile((String)inv);
            if (inv instanceof Closure) new InvHandler(invExecutor).call((Closure)inv);
        }

        report = invExecutor.execute();
    }

    private void loadInvScriptfile(String invScriptfile) {
        if (StringUtils.isEmpty(invScriptfile)) throw new IllegalArgumentException("Inv must be a valid non-null, non-empty value");

        File invFile = new File(invScriptfile);

        if (!invFile.exists()) {
            String testClassLocation = (String)getMetaClass().getProperty(this, "$0");
            invFile = new File(new File(testClassLocation).getParentFile(), invScriptfile);
        }

        if (!invFile.exists()) {
            URL location = this.getClass().getResource(invScriptfile);
            if (location != null)
                invFile = new File(location.getPath());
        }

        if (!invFile.exists())
            throw new IllegalStateException(invFile.getAbsolutePath() + " does not exists on the filesystem");

        InvInvoker.invoke(invExecutor, invFile);
    }
}
