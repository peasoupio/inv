package io.peasoup.inv.scm;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.MissingMethodException;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.Arrays;

public class ScmHandler {
    public ScmHandler(ScmExecutor executor, File parametersFile) {
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }

        this.executor = executor;
        this.parametersFile = parametersFile;
    }

    public ScmHandler(ScmExecutor executor) {
        this(executor, null);
    }

    @SuppressWarnings("rawtypes")
    public void call(@DelegatesTo(ScmDescriptor.class) Closure body) throws IllegalAccessException, SCMOptionRequiredException {
        if (body == null) {
            throw new IllegalArgumentException("body");
        }

        ScmDescriptor scmConfigDescriptor = new ScmDescriptor(parametersFile);

        body.setResolveStrategy(Closure.DELEGATE_ONLY);
        body.setDelegate(scmConfigDescriptor);

        try {
            body.call();
        } catch (MissingMethodException ex) {
            throw new IllegalAccessException("Scm instruction '" + ex.getMethod() + "' not found for arguments: " + Arrays.toString(ex.getArguments()));
        }

        if (StringUtils.isEmpty(scmConfigDescriptor.getName()))
            throw new SCMOptionRequiredException("path");

        if (scmConfigDescriptor.getPath() == null)
            throw new SCMOptionRequiredException("path");

        executor.add(scmConfigDescriptor);
    }

    private final ScmExecutor executor;
    private final File parametersFile;

    public static class SCMOptionRequiredException extends Exception {
        public SCMOptionRequiredException(final String option) {
            super("Option " + option + " is not valid. Please visit " + HELP_LINK + " for more information");
        }

        private static final String HELP_LINK = "https://github.com/peasoupio/inv/wiki/SCM-Syntax";
    }
}
