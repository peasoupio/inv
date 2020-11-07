package io.peasoup.inv.repo;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.MissingMethodException;
import io.peasoup.inv.MissingOptionException;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.Arrays;

public class RepoHandler {

    private static final String HELP_LINK = "https://github.com/peasoupio/inv/wiki/REPO-groovy-Syntax";

    private final File scriptFile;

    public RepoHandler(RepoExecutor executor, File scriptFile, File parametersFile) {
        if (executor == null)
            throw new IllegalArgumentException("executor");
        if (scriptFile == null)
            throw new IllegalArgumentException("scriptFile");

        this.executor = executor;
        this.scriptFile = scriptFile;
        this.parametersFile = parametersFile;
    }

    public RepoHandler(RepoExecutor executor, File scriptFile) {
        this(executor, scriptFile, null);
    }

    @SuppressWarnings("rawtypes")
    public void call(@DelegatesTo(RepoDescriptor.class) Closure body) throws IllegalAccessException, MissingOptionException {
        if (body == null) {
            throw new IllegalArgumentException("body");
        }

        RepoDescriptor repoDescriptor = new RepoDescriptor(scriptFile, parametersFile);

        body.setResolveStrategy(Closure.DELEGATE_ONLY);
        body.setDelegate(repoDescriptor);

        try {
            body.call();
        } catch (MissingMethodException ex) {
            throw new IllegalAccessException("Repo instruction '" + ex.getMethod() + "' not found for arguments: " + Arrays.toString(ex.getArguments()));
        }

        if (StringUtils.isEmpty(repoDescriptor.getName()))
            throw new MissingOptionException("name", HELP_LINK);

        if (repoDescriptor.getPath() == null)
            throw new MissingOptionException("repo.path", HELP_LINK);

        executor.add(repoDescriptor);
    }

    private final RepoExecutor executor;
    private final File parametersFile;


}
