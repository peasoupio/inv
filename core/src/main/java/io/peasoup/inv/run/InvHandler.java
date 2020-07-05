package io.peasoup.inv.run;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.Script;
import org.apache.commons.lang.StringUtils;

@SuppressWarnings("rawtypes")
public class InvHandler {
    private final InvExecutor invExecutor;

    private String scriptPath;
    private String pwd;
    private String scm;

    public InvHandler(InvExecutor invExecutor) {
        if (invExecutor == null) throw new IllegalArgumentException("invExecutor");

        this.invExecutor = invExecutor;
    }

    public InvHandler(InvExecutor invExecutor, String scriptPath, String pwd, String scm) {
        if (invExecutor == null) throw new IllegalArgumentException("invExecutor");
        if (scriptPath == null) throw new IllegalArgumentException("scriptPath");
        if (StringUtils.isEmpty(pwd)) throw new IllegalArgumentException("pwd");
        if (StringUtils.isEmpty(scm)) throw new IllegalArgumentException("scm");

        this.invExecutor = invExecutor;
        this.scriptPath = scriptPath;
        this.pwd = pwd;
        this.scm = scm;
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

            // Set SCM
            if (StringUtils.isNotEmpty(scm))
                context.setSCM(scm);

            // Set Script filename
            if (StringUtils.isNotEmpty(scriptPath))
                context.setBaseFilename(scriptPath);
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

        // Print SCM reference
        if (script != null) {
            Logger.info("[" + context.getScm() + "] [" + context.getBaseFilename() + "] " + inv);
        }
    }

    public static class INVOptionRequiredException extends Exception {
        private static final String HELP_LINK = "https://github.com/peasoupio/inv/wiki/INV-Syntax";

        public INVOptionRequiredException(final String option) {
            super("Option " + option + " is not valid. Please visit " + HELP_LINK + " for more information");
        }
    }
}
