package io.peasoup.inv.run;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import io.peasoup.inv.Logger;
import io.peasoup.inv.MissingOptionException;
import org.apache.commons.lang.StringUtils;

import java.io.File;

@SuppressWarnings("rawtypes")
public class InvHandler {

    private static final String HELP_LINK = "https://github.com/peasoupio/inv/wiki/INV-groovy-Syntax";

    private final InvExecutor invExecutor;

    private File scriptFile;
    private String pwd;
    private String repo;

    public InvHandler(InvExecutor invExecutor) {
        if (invExecutor == null) throw new IllegalArgumentException("invExecutor");

        this.invExecutor = invExecutor;
    }

    public InvHandler(InvExecutor invExecutor, File scriptFile, String pwd, String repo) {
        if (invExecutor == null) throw new IllegalArgumentException("invExecutor");
        if (scriptFile == null) throw new IllegalArgumentException("scriptFile");
        if (StringUtils.isEmpty(pwd)) throw new IllegalArgumentException("pwd");
        if (StringUtils.isEmpty(repo)) throw new IllegalArgumentException("repo");

        this.invExecutor = invExecutor;
        this.scriptFile = scriptFile;
        this.pwd = pwd;
        this.repo = repo;
    }

    public void call(@DelegatesTo(InvDescriptor.class) Closure body) throws MissingOptionException {
        if (body == null) {
            throw new IllegalArgumentException("Body is required");
        }

        this.call(body, body.getOwner().getClass().getSimpleName());
    }

    public void call(@DelegatesTo(InvDescriptor.class) Closure body, String defaultName) throws MissingOptionException {
        if (body == null) {
            throw new IllegalArgumentException("Body is required");
        }

        Inv.Context context = new Inv.Context(invExecutor.getPool());

        // Set default name
        if (StringUtils.isNotEmpty(defaultName)) context.setDefaultName(defaultName);

        // Is loading from script ?
        if (scriptFile != null) {
            // Set default path
            if (StringUtils.isNotEmpty(pwd))
                context.setDefaultPath(pwd);

            // Set REPO
            if (StringUtils.isNotEmpty(repo))
                context.setRepo(repo);

            // Set Script filename
            context.setBaseFilename(scriptFile.getAbsolutePath());
        }

        final Inv inv = context.build();
        body.setResolveStrategy(Closure.DELEGATE_FIRST);
        body.setDelegate(inv.getDelegate());

        try {
            body.run();
        } catch (Exception ex) {
            invExecutor.getReport().getErrors().add(new PoolReport.PoolError(inv, ex));
        }

        // Make sure, at any cost, delegate.name is not empty before dumping for the first time
        if (StringUtils.isEmpty(inv.getDelegate().getName()))
            throw new MissingOptionException("inv.name", HELP_LINK);

        // Attempt to dump delegate to insert it into pool
        inv.dumpDelegate();

        // Print REPO reference
        if (scriptFile != null) {
            Logger.info("[" + context.getRepo() + "] [" + context.getBaseFilename() + "] " + inv);
        }
    }
}
