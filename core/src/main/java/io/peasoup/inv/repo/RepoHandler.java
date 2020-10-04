package io.peasoup.inv.repo;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.MissingMethodException;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.Arrays;

public class RepoHandler {
    public RepoHandler(RepoExecutor executor, File parametersFile) {
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }

        this.executor = executor;
        this.parametersFile = parametersFile;
    }

    public RepoHandler(RepoExecutor executor) {
        this(executor, null);
    }

    @SuppressWarnings("rawtypes")
    public void call(@DelegatesTo(RepoDescriptor.class) Closure body) throws IllegalAccessException, RepoOptionRequiredException {
        if (body == null) {
            throw new IllegalArgumentException("body");
        }

        RepoDescriptor repoDescriptor = new RepoDescriptor(parametersFile);

        body.setResolveStrategy(Closure.DELEGATE_ONLY);
        body.setDelegate(repoDescriptor);

        try {
            body.call();
        } catch (MissingMethodException ex) {
            throw new IllegalAccessException("Repo instruction '" + ex.getMethod() + "' not found for arguments: " + Arrays.toString(ex.getArguments()));
        }

        if (StringUtils.isEmpty(repoDescriptor.getName()))
            throw new RepoOptionRequiredException("path");

        if (repoDescriptor.getPath() == null)
            throw new RepoOptionRequiredException("path");

        executor.add(repoDescriptor);
    }

    private final RepoExecutor executor;
    private final File parametersFile;

    public static class RepoOptionRequiredException extends Exception {
        public RepoOptionRequiredException(final String option) {
            super("Option " + option + " is not valid. Please visit " + HELP_LINK + " for more information");
        }

        private static final String HELP_LINK = "https://github.com/peasoupio/inv/wiki/REPO-groovy-Syntax";
    }
}
