package io.peasoup.inv.run.yaml;


import groovy.text.GStringTemplateEngine;
import groovy.text.TemplateEngine;
import io.peasoup.inv.run.*;
import io.peasoup.inv.loader.GroovyLoader;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.*;

public class YamlHandler {

    private final TemplateEngine engine = new GStringTemplateEngine();

    private final InvExecutor invExecutor;
    private final String yamlPath;
    private final String pwd;
    private final String scm;

    public YamlHandler(InvExecutor invExecutor, String yamlPath, String pwd, String scm) {
        if (invExecutor == null) throw new IllegalArgumentException("invExecutor");
        if (StringUtils.isEmpty(yamlPath)) throw new IllegalArgumentException("scriptPath");
        if (StringUtils.isEmpty(pwd)) throw new IllegalArgumentException("pwd");
        if (StringUtils.isEmpty(scm)) throw new IllegalArgumentException("scm");

        this.invExecutor = invExecutor;
        this.yamlPath = yamlPath;
        this.pwd = pwd;
        this.scm = scm;
    }

    public void call(YamlDescriptor yamlDescriptor) throws InvHandler.INVOptionRequiredException {
        Inv.Context context = new Inv.Context(invExecutor.getPool());

        // Set default path
        context.setDefaultPath(pwd);

        // Set SCM
        context.setSCM(scm);

        // Set Script filename
        context.setBaseFilename(yamlPath);

        final Inv inv = context.build();

        try {
            // Parse descriptor into inv object
            if (!parseDescriptor(yamlDescriptor, inv)) {
                throw new IllegalStateException("Cannot parse descriptor");
            }

        } catch (Exception ex) {
            invExecutor.getReport().getErrors().add(new PoolReport.PoolError(inv, ex));
            return;
        }

        // Make sure, at any cost, delegate.name is not empty before dumping for the first time
        if (StringUtils.isEmpty(inv.getDelegate().getName()))
            throw new InvHandler.INVOptionRequiredException("name");

        // Attempt to dump delegate to insert it into pool
        inv.dumpDelegate();

        // Print SCM reference
        Logger.info("[" + context.getScm() + "] [" + context.getBaseFilename() + "] " + inv);

    }

    private boolean parseDescriptor(YamlDescriptor descriptor, Inv inv) throws IOException, ClassNotFoundException {

        InvDescriptor delegate = inv.getDelegate();

        // Sets name
        if (StringUtils.isNotEmpty(descriptor.getName()))
            delegate.name(descriptor.getName());

        // Sets path
        if (StringUtils.isNotEmpty(descriptor.getPath()))
            delegate.path(descriptor.getPath());

        Map<String, String> interpolatable = new HashMap<>();
        interpolatable.put("name", delegate.getName());
        interpolatable.put("path", delegate.getPath());

        // Sets tags
        if (descriptor.getTags() != null) {
            // Interpolate tags
            Map interpolated = interpolateMap(descriptor.getTags(), interpolatable);

            // Sets and add interpolated tags as interpolatable
            delegate.tags(interpolated);
            interpolatable.putAll(interpolated);
        }


        // If workflow is not defined, do not proceed
        if (descriptor.getWorkflow() == null || descriptor.getWorkflow().isEmpty())
            return true;

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


        return true;
    }

    private void parseBroadcastStatement(YamlBroadcastStatementDescriptor descriptor, Inv inv, Map<String, String> interpolatable) throws IOException, ClassNotFoundException {
        StatementDescriptor statementDescriptor = new StatementDescriptor(descriptor.getName());
        BroadcastUsingDescriptor broadcastUsingDescriptor = new BroadcastUsingDescriptor();

        if (descriptor.getId() != null)
            broadcastUsingDescriptor.id(interpolate(descriptor.getId(), interpolatable));

        if (StringUtils.isNotEmpty(descriptor.getMarkdown()))
            broadcastUsingDescriptor.markdown((String)interpolate(descriptor.getMarkdown(), interpolatable));

        if (StringUtils.isNotEmpty(descriptor.getReady()))
            broadcastUsingDescriptor.ready(new LazyYamlClosure(inv, descriptor.getReady()));

        BroadcastDescriptor broadcastDescriptor = inv.getDelegate().broadcast(statementDescriptor);
        broadcastDescriptor.using(broadcastUsingDescriptor);
    }

    private void parseRequireStatement(YamlRequireStatementDescriptor descriptor, Inv inv, Map<String, String> interpolatable) throws IOException, ClassNotFoundException {
        StatementDescriptor statementDescriptor = new StatementDescriptor(descriptor.getName());
        RequireUsingDescriptor requireUsingDescriptor = new RequireUsingDescriptor();

        if (descriptor.getId() != null)
            requireUsingDescriptor.id(interpolate(descriptor.getId(), interpolatable));

        if (StringUtils.isNotEmpty(descriptor.getMarkdown()))
            requireUsingDescriptor.markdown((String)interpolate(descriptor.getMarkdown(), interpolatable));

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

    private Object interpolate(Object receiver, Map<String, String> data) throws IOException, ClassNotFoundException {
        if (receiver instanceof String)
            return interpolateString((String)receiver, data);

        if (receiver instanceof Map<?,?>)
            return interpolateMap((Map)receiver, data);

        return receiver;
    }

    private Map<String, String> interpolateMap(Map receiver, Map<String, String> data) throws IOException, ClassNotFoundException {
        if (receiver.isEmpty())
            return new HashMap<>();

        Map<String, String> interpolated = new HashMap<>();

        for(Map.Entry<?,?> entry : (Set<Map.Entry>)receiver.entrySet()) {

            // Do not proceed if key or value is not a String type
            if (!(entry.getKey() instanceof String))
                continue;

            if (!(entry.getValue() instanceof String))
                continue;

            interpolated.put(
                    engine.createTemplate((String)entry.getKey()).make(data).toString(),
                    engine.createTemplate((String)entry.getValue()).make(data).toString()
            );
        }

        return interpolated;
    }

    private String interpolateString(String receiver, Map<String, String> data) throws IOException, ClassNotFoundException {
        if (receiver.isEmpty())
            return "";

        return engine.createTemplate(receiver).make(data).toString();
    }




}
