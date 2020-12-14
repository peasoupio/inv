package io.peasoup.inv.loader;

import groovy.text.GStringTemplateEngine;
import groovy.text.TemplateEngine;
import io.peasoup.inv.repo.yaml.YamlRepoDescriptor;
import io.peasoup.inv.run.yaml.YamlInvDescriptor;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class YamlLoader {

    private final TemplateEngine engine;
    private final Yaml yaml;

    public YamlLoader() {
        this.engine = new GStringTemplateEngine();
        this.yaml = new Yaml(new Constructor(YamlLoader.Descriptor.class));
    }

    /**
     * Parse a Yaml script file
     * @param scriptFile Script file location
     * @return A new YamlLoader.Descriptor instance
     * @throws IOException
     */
    public YamlLoader.Descriptor parseYamlFile(File scriptFile) throws IOException {
        return parseYamlReader(ResourceGroovyMethods.newReader(scriptFile));
    }

    public YamlLoader.Descriptor parseYamlText(String scriptText) throws IOException {
        return parseYamlReader(new BufferedReader(new StringReader(scriptText)));
    }

    private YamlLoader.Descriptor parseYamlReader(BufferedReader reader) throws IOException {
        try(reader) {
            try {
                return yaml.load(reader);
            } catch(Exception ex) {
                throw new IOException(StackTraceUtils.sanitize(ex));
            }
        }
    }


    public Object interpolate(Object receiver, Map<String, String> data) throws IOException, ClassNotFoundException {
        if (receiver instanceof String)
            return interpolateString((String)receiver, data);

        if (receiver instanceof Map<?,?>)
            return interpolateMap((Map)receiver, data);

        return receiver;
    }

    public Map<String, String> interpolateMap(Map receiver, Map<String, String> data) throws IOException, ClassNotFoundException {
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

    public List interpolateList(List receiver, Map<String, String> data) throws IOException, ClassNotFoundException {
        List interpolatedValues = new ArrayList<>();

        // Iterate through elements
        for(Object value : receiver) {
            interpolatedValues.add(interpolate(value, data));
        }

        return interpolatedValues;
    }

    public String interpolateString(String receiver, Map<String, String> data) throws IOException, ClassNotFoundException {
        if (StringUtils.isEmpty(receiver))
            return receiver;

        return engine.createTemplate(receiver).make(data).toString();
    }

    /**
     * Yaml is designed with a single descriptor.
     * It allows multiple definitations in the same file.
     */
    public static class Descriptor {

        private List<YamlInvDescriptor> inv;

        private List<YamlRepoDescriptor> repo;

        public List<YamlInvDescriptor> getInv() {
            return inv;
        }

        public void setInv(List<YamlInvDescriptor> inv) {
            this.inv = inv;
        }

        public List<YamlRepoDescriptor> getRepo() {
            return repo;
        }

        public void setRepo(List<YamlRepoDescriptor> repo) {
            this.repo = repo;
        }
    }
}
