package io.peasoup.inv.run.yaml;

import io.peasoup.inv.Logger;
import io.peasoup.inv.MissingOptionException;
import io.peasoup.inv.loader.LazyYamlClosure;
import io.peasoup.inv.loader.YamlLoader;
import io.peasoup.inv.repo.RepoLoadHandler;
import io.peasoup.inv.run.*;
import lombok.NonNull;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class YamlInvHandler {

    private static final String HELP_LINK = "https://github.com/peasoupio/inv/wiki/INV-yaml-Syntax";

    private final InvExecutor invExecutor;
    private final YamlLoader yamlLoader;
    private final File yamlFile;
    private final String pwd;
    private final String repo;

    private final RepoLoadHandler repoLoadHandler;

    public YamlInvHandler(
            @NonNull InvExecutor invExecutor,
            @NonNull YamlLoader yamlLoader,
            @NonNull File yamlFile,
            @NonNull String pwd,
            @NonNull String repo,
            @NonNull RepoLoadHandler repoLoadHandler) {
        if (StringUtils.isEmpty(pwd)) throw new IllegalArgumentException("pwd");
        if (StringUtils.isEmpty(repo)) throw new IllegalArgumentException("repo");

        this.invExecutor = invExecutor;
        this.yamlLoader = yamlLoader;
        this.yamlFile = yamlFile;
        this.pwd = pwd;
        this.repo = repo;

        this.repoLoadHandler = repoLoadHandler;
    }

    public void call() throws MissingOptionException, IOException {
        YamlLoader.Descriptor descriptor = yamlLoader.parseYamlFile(yamlFile);

        // Skip if cannot get YamlInvDescriptor's
        if (descriptor == null ||
                descriptor.getInv() == null ||
                descriptor.getInv().isEmpty())
            return;

        Inv.Context context = new Inv.Context(invExecutor.getPool());

        // Set default name
        context.setDefaultName(yamlFile.getName().split("\\.")[0]);

        // Set default path
        context.setDefaultPath(pwd);

        // Set REPO
        context.setRepo(repo);

        // Set Script filename
        context.setBaseFilename(yamlFile.getAbsolutePath());

        // Process "get" statements
        if (descriptor.getLoad() != null) {
            for (String src : descriptor.getLoad()) {
                repoLoadHandler.call(src);
            }
        }

        // Process YAML INV descriptor into real INV instances
        for (YamlInvDescriptor yamlInvDescriptor : descriptor.getInv()) {
            final Inv inv = context.build();

            try {
                // Parse descriptor into inv object
                parseDescriptor(yamlInvDescriptor, inv);

            } catch (Exception ex) {
                invExecutor.getReport().getErrors().add(new PoolReport.PoolError(inv, ex));
                return;
            }

            // Make sure, at any cost, delegate.name is not empty before dumping for the first time
            if (StringUtils.isEmpty(inv.getDelegate().getName()))
                throw new MissingOptionException("inv.name", HELP_LINK);

            // Attempt to dump delegate to insert it into pool
            inv.dumpDelegate();

            // Print REPO reference
            Logger.info("[" + context.getRepo() + "] [" + context.getBaseFilename() + "] " + inv);
        }
    }

    private void parseDescriptor(YamlInvDescriptor descriptor, Inv inv) throws IOException, ClassNotFoundException {

        InvDescriptor delegate = inv.getDelegate();

        // Sets name
        if (StringUtils.isNotEmpty(descriptor.getName()))
            delegate.name(descriptor.getName());

        // Sets path
        if (StringUtils.isNotEmpty(descriptor.getPath()))
            delegate.path(descriptor.getPath());

        // Sets markdown
        if (StringUtils.isNotEmpty(descriptor.getMarkdown()))
            delegate.markdown(descriptor.getMarkdown());

        Map<String, String> interpolatable = new HashMap<>();
        interpolatable.put("$0", delegate.get$0());
        interpolatable.put("name", delegate.getName());
        interpolatable.put("path", delegate.getPath());

        // Sets tags
        if (descriptor.getTags() != null) {

            // Interpolate tags
            Map interpolated = yamlLoader.interpolateMap(descriptor.getTags(), interpolatable);

            // Sets and add interpolated tags as interpolatable
            delegate.tags(interpolated);
            interpolatable.putAll(interpolated);
        }


        // If workflow is not defined, do not proceed
        if (descriptor.getWorkflow() == null || descriptor.getWorkflow().isEmpty())
            return;

        // Validate all statetement descriptors
        Queue<YamlStatementDescriptor> statementDescriptors = new LinkedList<>(descriptor.getWorkflow());
        while(!statementDescriptors.isEmpty()) {
            // Get next statement descriptor
            YamlStatementDescriptor statementDescriptor = statementDescriptors.poll();
            if (statementDescriptor == null)
                break;

            // Process broadcasts
            if (statementDescriptor.getBroadcast() != null)
                parseBroadcastStatement(statementDescriptor.getBroadcast(), inv, interpolatable);

            // Process requires
            if (statementDescriptor.getRequire() != null)
                parseRequireStatement(statementDescriptor.getRequire(), inv, interpolatable);
        }
    }

    private void parseBroadcastStatement(YamlBroadcastStatementDescriptor descriptor, Inv inv, Map<String, String> interpolatable) throws IOException, ClassNotFoundException {
        StatementDescriptor statementDescriptor = new StatementDescriptor(descriptor.getName());
        BroadcastUsingDescriptor broadcastUsingDescriptor = new BroadcastUsingDescriptor();

        if (descriptor.getId() != null)
            broadcastUsingDescriptor.id(yamlLoader.interpolate(descriptor.getId(), interpolatable));

        if (StringUtils.isNotEmpty(descriptor.getMarkdown()))
            broadcastUsingDescriptor.markdown((String)yamlLoader.interpolate(descriptor.getMarkdown(), interpolatable));

        if (StringUtils.isNotEmpty(descriptor.getReady()))
            broadcastUsingDescriptor.ready(new LazyYamlClosure(inv, descriptor.getReady()));

        BroadcastDescriptor broadcastDescriptor = inv.getDelegate().broadcast(statementDescriptor);
        broadcastDescriptor.using(broadcastUsingDescriptor);
    }

    private void parseRequireStatement(YamlRequireStatementDescriptor descriptor, Inv inv, Map<String, String> interpolatable) throws IOException, ClassNotFoundException {
        StatementDescriptor statementDescriptor = new StatementDescriptor(descriptor.getName());
        RequireUsingDescriptor requireUsingDescriptor = new RequireUsingDescriptor();

        if (descriptor.getId() != null)
            requireUsingDescriptor.id(yamlLoader.interpolate(descriptor.getId(), interpolatable));

        if (StringUtils.isNotEmpty(descriptor.getMarkdown()))
            requireUsingDescriptor.markdown((String) yamlLoader.interpolate(descriptor.getMarkdown(), interpolatable));

        if (descriptor.getUnbloatable() != null)
            requireUsingDescriptor.unbloatable(descriptor.getUnbloatable());

        if (descriptor.getDefaults() != null)
            requireUsingDescriptor.defaults(descriptor.getDefaults());

        if (StringUtils.isNotEmpty(descriptor.getResolved()))
            requireUsingDescriptor.resolved(new LazyYamlClosure(inv, descriptor.getResolved()));

        if (StringUtils.isNotEmpty(descriptor.getUnresolved()))
            requireUsingDescriptor.unresolved(new LazyYamlClosure(inv, descriptor.getUnresolved()));

        RequireDescriptor requireDescriptor = inv.getDelegate().require(statementDescriptor);
        requireDescriptor.using(requireUsingDescriptor);
    }
}
