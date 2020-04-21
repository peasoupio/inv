package io.peasoup.inv.run;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.Script;
import org.apache.commons.lang.StringUtils;

public class InvHandler {
    private final InvExecutor executor;

    public InvHandler(InvExecutor executor) {
        if (executor == null) {
            throw new IllegalArgumentException("Executor is required to handle INV script(s)");
        }

        this.executor = executor;
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

        Inv.Context context = new Inv.Context(executor.getPool());

        // Set default name
        if (StringUtils.isNotEmpty(defaultName)) context.setDefaultName(defaultName);

        // Is loading from script ?
        Script script = (body.getOwner() instanceof Script) ? (Script) body.getOwner() : null;
        if (script != null) {
            // Set default path
            context.setDefaultPath((String) script.getBinding().getVariables().get("pwd"));

            // Set SCM
            context.setSCM((String) script.getBinding().getVariables().get("scm"));

            // Set Script filename
            context.setScriptFilename((String) script.getBinding().getVariables().get("$0"));
        }

        final Inv inv = context.build();
        body.setResolveStrategy(Closure.DELEGATE_FIRST);
        body.setDelegate(inv.getDelegate());

        try {
            body.call();
        } catch (Exception ex) {
            executor.getReport().getErrors().add(new PoolReport.PoolError(inv, ex));
        }

        // Make sure, at any cost, delegate.name is not empty before dumping for the first time
        if (StringUtils.isEmpty(inv.getDelegate().getName()))
            throw new INVOptionRequiredException("name");

        // Attempt to dump delegate to insert it into pool
        inv.dumpDelegate();

        // Print SCM reference
        if (script != null) {
            Logger.info("[" + context.getScm() + "] [" + context.getScriptFilename() + "] " + inv);
        }
    }

    public static class INVOptionRequiredException extends Exception {
        private static final String HELP_LINK = "https://github.com/peasoupio/inv/wiki/INV-Syntax";

        public INVOptionRequiredException(final String option) {
            super("Option " + option + " is not valid. Please visit " + HELP_LINK + " for more information");
        }
    }
}
