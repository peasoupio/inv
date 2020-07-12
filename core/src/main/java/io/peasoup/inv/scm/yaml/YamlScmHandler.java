package io.peasoup.inv.scm.yaml;


import io.peasoup.inv.loader.YamlLoader;
import io.peasoup.inv.run.*;
import io.peasoup.inv.scm.ScmDescriptor;
import io.peasoup.inv.scm.ScmExecutor;
import io.peasoup.inv.scm.ScmHandler;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class YamlScmHandler {

    private final ScmExecutor scmExecutor;
    private final File parametersFile;

    public YamlScmHandler(ScmExecutor scmExecutor, File parametersFile) {
        if (scmExecutor == null) throw new IllegalArgumentException("scmExecutor");

        this.scmExecutor = scmExecutor;
        this.parametersFile = parametersFile;
    }

    public void call(YamlLoader.Descriptor yamlLoader) {

        // Skip if cannot get YamlInvDescriptor's
        if (yamlLoader == null ||
                yamlLoader.getScm() == null ||
                yamlLoader.getScm().isEmpty())
            return;

        for (YamlScmDescriptor yamlScmDescriptor : yamlLoader.getScm()) {

            final ScmDescriptor scm = new ScmDescriptor(parametersFile);

            try {
                // Parse descriptor into inv object
                parseDescriptor(yamlScmDescriptor, scm);

                // Add resolved SCM to the executor
                scmExecutor.add(scm);

            } catch (Exception ex) {
                Logger.error(ex);
                return;
            }
        }
    }

    private void parseDescriptor(YamlScmDescriptor descriptor, ScmDescriptor scm) throws IOException, ClassNotFoundException, ScmHandler.SCMOptionRequiredException {

        Map<String, String> interpolatable = new HashMap<>();
        interpolatable.putAll(ScmDescriptor.getEnv());

        // Sets name
        if (StringUtils.isNotEmpty(descriptor.getName()))
            scm.name(YamlLoader.interpolateString(descriptor.getName(), interpolatable));
        interpolatable.put("name", scm.getName());

        // Sets path
        if (StringUtils.isNotEmpty(descriptor.getPath()))
            scm.path(YamlLoader.interpolateString(descriptor.getPath(), interpolatable));
        interpolatable.put("path", scm.getPath().getAbsolutePath());

        // Sets src
        if (StringUtils.isNotEmpty(descriptor.getSrc()))
            scm.src(YamlLoader.interpolateString(descriptor.getSrc(), interpolatable));
        interpolatable.put("src", scm.getPath().getAbsolutePath());

        // Sets entry
        if (StringUtils.isNotEmpty(descriptor.getEntry()))
            scm.entry(YamlLoader.interpolateString(descriptor.getEntry(), interpolatable));

        parseAsk(descriptor, scm, interpolatable);
        parseHooks(descriptor, scm, interpolatable);

    }

    private void parseAsk(YamlScmDescriptor descriptor, ScmDescriptor scm, Map<String, String> interpolatable) throws IOException, ClassNotFoundException, ScmHandler.SCMOptionRequiredException {
        // Sets ask parameters
        if (descriptor.getAsk() != null) {

            for (YamlAskDescriptor askDescriptor : descriptor.getAsk()) {
                YamlParameterDescriptor parameter = askDescriptor.getParameter();

                Map options = new HashMap<String, Object>();

                if (StringUtils.isNotEmpty(parameter.getDefaultValue()))
                    options.put("defaultValue", YamlLoader.interpolateString(parameter.getDefaultValue(), interpolatable));

                if (StringUtils.isNotEmpty(parameter.getCommand()))
                    options.put("command", YamlLoader.interpolateString(parameter.getCommand(), interpolatable));

                if (parameter.getValues() != null)
                    options.put("values", YamlLoader.interpolateList(parameter.getValues(), interpolatable));

                if (StringUtils.isNotEmpty(parameter.getFilterRegex()))
                    options.put("filterRegex", parameter.getFilterRegex());

                // Create the actual parameter
                scm.getAsk().parameter(
                        parameter.getName(),
                        parameter.getDescription(),
                        options);
            }
        }
    }

    private void parseHooks(YamlScmDescriptor descriptor, ScmDescriptor scm, Map<String, String> interpolatable) throws IOException, ClassNotFoundException {
        // Sets hooks
        if (descriptor.getHooks() != null) {

            if (StringUtils.isNotEmpty(descriptor.getHooks().getInit()))
                scm.getHooks().init(YamlLoader.interpolateString(descriptor.getHooks().getInit(), interpolatable));

            if (StringUtils.isNotEmpty(descriptor.getHooks().getPull()))
                scm.getHooks().pull(YamlLoader.interpolateString(descriptor.getHooks().getPull(), interpolatable));

            if (StringUtils.isNotEmpty(descriptor.getHooks().getPush()))
                scm.getHooks().push(YamlLoader.interpolateString(descriptor.getHooks().getPush(), interpolatable));

            if (StringUtils.isNotEmpty(descriptor.getHooks().getVersion()))
                scm.getHooks().version(YamlLoader.interpolateString(descriptor.getHooks().getVersion(), interpolatable));

            if (StringUtils.isNotEmpty(descriptor.getHooks().getUpdate()))
                scm.getHooks().update(YamlLoader.interpolateString(descriptor.getHooks().getUpdate(), interpolatable));
        }
    }


}

