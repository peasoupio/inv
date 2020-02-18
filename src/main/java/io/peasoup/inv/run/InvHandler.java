package io.peasoup.inv.run;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.Script;
import org.apache.commons.lang.StringUtils;

public class InvHandler {
    private final InvExecutor executor;

    public InvHandler(InvExecutor executor) {
        assert executor != null : "Executor is required to handle INV script(s)";

        this.executor = executor;
    }

    public void call(@DelegatesTo(InvDescriptor.class) Closure body) throws INVOptionRequiredException {
        assert body != null : "Body is required";

        this.call(body, body.getOwner().getClass().getSimpleName());
    }

    public void call(@DelegatesTo(InvDescriptor.class) Closure body, String defaultName) throws INVOptionRequiredException {
        assert body != null : "Body is required";

        final Inv inv = new Inv(executor.getPool());

        // Is loading from script ?
        Script script = (body.getOwner() instanceof Script) ? (Script) body.getOwner() : null;

        // Set default path
        if (script != null) inv.getDelegate().path((String) script.getBinding().getVariables().get("pwd"));
        // Set default name
        if (StringUtils.isNotEmpty(defaultName)) inv.getDelegate().name(defaultName);

        body.setResolveStrategy(Closure.DELEGATE_FIRST);
        body.setDelegate(inv.getDelegate());

        try {
            body.call();
        } catch (Exception ex) {
            PoolReport.PoolException exception = new PoolReport.PoolException();
            exception.setInv(inv);
            exception.setException(ex);

            executor.getReport().getExceptions().add(exception);
        }

        // Make sure, at any cost, delegate.name is not empty before dumping for the first time
        if (StringUtils.isEmpty(inv.getDelegate().getName()))
            throw new INVOptionRequiredException("name");

        // Attempt to dump delegate to insert it into pool
        inv.dumpDelegate();

        if (script != null) {
            String scm = (String) script.getBinding().getVariables().get("scm");
            final String file = (String) script.getBinding().getVariables().get("$0");

            Logger.info("[" + scm + "] [" + file + "] [" + inv.getName() + "]");
        }

    }

    public static class INVOptionRequiredException extends Exception {
        private static final String HELP_LINK = "https://github.com/peasoupio/inv/wiki/INV-Syntax";

        public INVOptionRequiredException(final String option) {
            super("Option " + option + " is not valid. Please visit " + HELP_LINK + " for more information");
        }
    }
}
