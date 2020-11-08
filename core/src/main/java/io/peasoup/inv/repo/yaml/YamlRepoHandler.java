package io.peasoup.inv.repo.yaml;


import io.peasoup.inv.MissingOptionException;
import io.peasoup.inv.loader.InterpolableGroovyObject;
import io.peasoup.inv.loader.YamlLoader;
import io.peasoup.inv.repo.RepoDescriptor;
import io.peasoup.inv.repo.RepoExecutor;
import io.peasoup.inv.run.Logger;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class YamlRepoHandler {

    private static final String HELP_LINK = "https://github.com/peasoupio/inv/wiki/REPO-yaml-Syntax";

    private final RepoExecutor repoExecutor;
    private final YamlLoader yamlLoader;
    private final File yamlFile;
    private final File parametersFile;

    public YamlRepoHandler(RepoExecutor repoExecutor, YamlLoader yamlLoader, File yamlFile, File parametersFile) {
        if (repoExecutor == null) throw new IllegalArgumentException("repoExecutor");
        if (yamlLoader == null) throw new IllegalArgumentException("yamlLoader");
        if (yamlFile == null) throw new IllegalArgumentException("yamlFile");

        this.repoExecutor = repoExecutor;
        this.yamlLoader = yamlLoader;
        this.yamlFile = yamlFile;
        this.parametersFile = parametersFile;
    }

    public void call() throws IOException, MissingOptionException {
        YamlLoader.Descriptor descriptor = yamlLoader.parseYaml(yamlFile);

        // Skip if cannot get YamlInvDescriptor's
        if (descriptor == null ||
                descriptor.getRepo() == null ||
                descriptor.getRepo().isEmpty())
            return;

        for (YamlRepoDescriptor yamlRepoDescriptor : descriptor.getRepo()) {

            final RepoDescriptor repo = new RepoDescriptor(yamlFile, parametersFile);

            try {
                // Parse descriptor into inv object
                parseDescriptor(yamlRepoDescriptor, repo);
            } catch (Exception ex) {
                Logger.error(ex);
                return;
            }

            if (StringUtils.isEmpty(repo.getName()))
                throw new MissingOptionException("repo.name", HELP_LINK);

            if (repo.getPath() == null)
                throw new MissingOptionException("repo.path", HELP_LINK);

            // Add resolved REPO to the executor
            repoExecutor.add(repo);
        }
    }

    private void parseDescriptor(YamlRepoDescriptor descriptor, RepoDescriptor repo) throws IOException, ClassNotFoundException, MissingOptionException {
        InterpolableGroovyObject interpolatable = new InterpolableGroovyObject(repo);

        // Sets name
        if (StringUtils.isNotEmpty(descriptor.getName()))
            repo.name(yamlLoader.interpolateString(descriptor.getName(), interpolatable));

        // Sets path
        if (StringUtils.isNotEmpty(descriptor.getPath()))
            repo.path(yamlLoader.interpolateString(descriptor.getPath(), interpolatable));

        // Sets src
        if (StringUtils.isNotEmpty(descriptor.getSrc()))
            repo.src(yamlLoader.interpolateString(descriptor.getSrc(), interpolatable));

        parseAsk(descriptor, repo, interpolatable);
        parseHooks(descriptor, repo, interpolatable);

    }

    private void parseAsk(YamlRepoDescriptor descriptor, RepoDescriptor repo, Map<String, String> interpolatable) throws IOException, ClassNotFoundException, MissingOptionException {
        // Sets ask parameters
        if (descriptor.getAsk() != null) {

            for (YamlAskDescriptor askDescriptor : descriptor.getAsk()) {
                YamlParameterDescriptor parameter = askDescriptor.getParameter();

                Map<String, Object> options = new HashMap<>();

                if (StringUtils.isNotEmpty(parameter.getDefaultValue()))
                    options.put("defaultValue", yamlLoader.interpolateString(parameter.getDefaultValue(), interpolatable));

                if (StringUtils.isNotEmpty(parameter.getCommand()))
                    options.put("command", yamlLoader.interpolateString(parameter.getCommand(), interpolatable));

                if (parameter.getValues() != null)
                    options.put("values", yamlLoader.interpolateList(parameter.getValues(), interpolatable));

                if (StringUtils.isNotEmpty(parameter.getFilterRegex()))
                    options.put("filterRegex", parameter.getFilterRegex());

                // Create the actual parameter
                repo.getAsk().parameter(
                        parameter.getName(),
                        parameter.getUsage(),
                        options);
            }
        }
    }

    private void parseHooks(YamlRepoDescriptor descriptor, RepoDescriptor repo, Map<String, String> interpolatable) throws IOException, ClassNotFoundException {
        // Sets hooks
        if (descriptor.getHooks() != null) {

            if (StringUtils.isNotEmpty(descriptor.getHooks().getInit()))
                repo.getHooks().init(yamlLoader.interpolateString(descriptor.getHooks().getInit(), interpolatable));

            if (StringUtils.isNotEmpty(descriptor.getHooks().getPull()))
                repo.getHooks().pull(yamlLoader.interpolateString(descriptor.getHooks().getPull(), interpolatable));

            if (StringUtils.isNotEmpty(descriptor.getHooks().getPush()))
                repo.getHooks().push(yamlLoader.interpolateString(descriptor.getHooks().getPush(), interpolatable));

            if (StringUtils.isNotEmpty(descriptor.getHooks().getVersion()))
                repo.getHooks().version(yamlLoader.interpolateString(descriptor.getHooks().getVersion(), interpolatable));
        }
    }
}

