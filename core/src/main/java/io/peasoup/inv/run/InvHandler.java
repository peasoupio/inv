package io.peasoup.inv.run;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.Script;
import org.apache.commons.lang.StringUtils;

import java.io.File;

@SuppressWarnings("rawtypes")
public class InvHandler {
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

    public void call(@DelegatesTo(InvDescriptor.class) Closure body) throws INVOptionRequiredException {
        if (body == null) {
            throw new IllegalArgumentException("Body is required");
        }

        this.call(body, body.getOwner().getClass().getSimpleName());
    }

    public void call(@DelegatesTo(InvDescriptor.class) Closure body, String defaultName) throws INVOptionRequiredException {
        if (body == null) {
            throw new IllegalArgumentException("Body is required");
        }

        Inv.Context context = new Inv.Context(invExecutor.getPool());

        // Set default name
        if (StringUtils.isNotEmpty(defaultName)) context.setDefaultName(defaultName);

        // Is loading from script ?
        Script script = (body.getOwner() instanceof Script) ? (Script) body.getOwner() : null;
        if (script != null) {
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
            body.call();
        } catch (Exception ex) {
            invExecutor.getReport().getErrors().add(new PoolReport.PoolError(inv, ex));
        }

        // Make sure, at any cost, delegate.name is not empty before dumping for the first time
        if (StringUtils.isEmpty(inv.getDelegate().getName()))
            throw new INVOptionRequiredException("name");

        // Attempt to dump delegate to insert it into pool
        inv.dumpDelegate();

        // Print REPO reference
        if (script != null) {
            Logger.info("[" + context.getRepo() + "] [" + context.getBaseFilename() + "] " + inv);
        }
    }

    public static class INVOptionRequiredException extends Exception {
        private static final String HELP_LINK = "https://github.com/peasoupio/inv/wiki/INV-groovy-Syntax";

        public INVOptionRequiredException(final String option) {
            super("Option " + option + " is not valid. Please visit " + HELP_LINK + " for more information");
        }
    }
}
