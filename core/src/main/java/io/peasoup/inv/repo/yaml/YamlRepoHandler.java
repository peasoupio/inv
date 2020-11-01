package io.peasoup.inv.repo.yaml;


import io.peasoup.inv.loader.YamlLoader;
import io.peasoup.inv.repo.RepoDescriptor;
import io.peasoup.inv.repo.RepoExecutor;
import io.peasoup.inv.repo.RepoHandler;
import io.peasoup.inv.run.Logger;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class YamlRepoHandler {

    private final RepoExecutor repoExecutor;
    private final File parametersFile;

    public YamlRepoHandler(RepoExecutor repoExecutor, File parametersFile) {
        if (repoExecutor == null) throw new IllegalArgumentException("repoExecutor");

        this.repoExecutor = repoExecutor;
        this.parametersFile = parametersFile;
    }

    public void call(YamlLoader.Descriptor yamlLoader) {

        // Skip if cannot get YamlInvDescriptor's
        if (yamlLoader == null ||
                yamlLoader.getRepo() == null ||
                yamlLoader.getRepo().isEmpty())
            return;

        for (YamlRepoDescriptor yamlRepoDescriptor : yamlLoader.getRepo()) {

            final RepoDescriptor repo = new RepoDescriptor(parametersFile);

            try {
                // Parse descriptor into inv object
                parseDescriptor(yamlRepoDescriptor, repo);

                // Add resolved REPO to the executor
                repoExecutor.add(repo);

            } catch (Exception ex) {
                Logger.error(ex);
                return;
            }
        }
    }

    private void parseDescriptor(YamlRepoDescriptor descriptor, RepoDescriptor repo) throws IOException, ClassNotFoundException, RepoHandler.RepoOptionRequiredException {

        Map<String, String> interpolatable = new HashMap<>(RepoDescriptor.getEnv());

        // Sets name
        if (StringUtils.isNotEmpty(descriptor.getName()))
            repo.name(YamlLoader.interpolateString(descriptor.getName(), interpolatable));
        interpolatable.put("name", repo.getName());

        // Sets path
        if (StringUtils.isNotEmpty(descriptor.getPath()))
            repo.path(YamlLoader.interpolateString(descriptor.getPath(), interpolatable));
        interpolatable.put("path", repo.getPath());

        // Sets src
        if (StringUtils.isNotEmpty(descriptor.getSrc()))
            repo.src(YamlLoader.interpolateString(descriptor.getSrc(), interpolatable));
        interpolatable.put("src", repo.getSrc());

        parseAsk(descriptor, repo, interpolatable);
        parseHooks(descriptor, repo, interpolatable);

    }

    private void parseAsk(YamlRepoDescriptor descriptor, RepoDescriptor repo, Map<String, String> interpolatable) throws IOException, ClassNotFoundException, RepoHandler.RepoOptionRequiredException {
        // Sets ask parameters
        if (descriptor.getAsk() != null) {

            for (YamlAskDescriptor askDescriptor : descriptor.getAsk()) {
                YamlParameterDescriptor parameter = askDescriptor.getParameter();

                Map<String, Object> options = new HashMap<>();

                if (StringUtils.isNotEmpty(parameter.getDefaultValue()))
                    options.put("defaultValue", YamlLoader.interpolateString(parameter.getDefaultValue(), interpolatable));

                if (StringUtils.isNotEmpty(parameter.getCommand()))
                    options.put("command", YamlLoader.interpolateString(parameter.getCommand(), interpolatable));

                if (parameter.getValues() != null)
                    options.put("values", YamlLoader.interpolateList(parameter.getValues(), interpolatable));

                if (StringUtils.isNotEmpty(parameter.getFilterRegex()))
                    options.put("filterRegex", parameter.getFilterRegex());

                // Create the actual parameter
                repo.getAsk().parameter(
                        parameter.getName(),
                        parameter.getDescription(),
                        options);
            }
        }
    }

    private void parseHooks(YamlRepoDescriptor descriptor, RepoDescriptor repo, Map<String, String> interpolatable) throws IOException, ClassNotFoundException {
        // Sets hooks
        if (descriptor.getHooks() != null) {

            if (StringUtils.isNotEmpty(descriptor.getHooks().getInit()))
                repo.getHooks().init(YamlLoader.interpolateString(descriptor.getHooks().getInit(), interpolatable));

            if (StringUtils.isNotEmpty(descriptor.getHooks().getPull()))
                repo.getHooks().pull(YamlLoader.interpolateString(descriptor.getHooks().getPull(), interpolatable));

            if (StringUtils.isNotEmpty(descriptor.getHooks().getPush()))
                repo.getHooks().push(YamlLoader.interpolateString(descriptor.getHooks().getPush(), interpolatable));

            if (StringUtils.isNotEmpty(descriptor.getHooks().getVersion()))
                repo.getHooks().version(YamlLoader.interpolateString(descriptor.getHooks().getVersion(), interpolatable));
        }
    }


}

