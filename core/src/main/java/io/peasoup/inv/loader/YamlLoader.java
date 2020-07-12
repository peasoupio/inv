package io.peasoup.inv.loader;

import groovy.text.GStringTemplateEngine;
import groovy.text.TemplateEngine;
import io.peasoup.inv.run.yaml.YamlInvDescriptor;
import io.peasoup.inv.scm.yaml.YamlScmDescriptor;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class YamlLoader {

    private static final TemplateEngine engine = new GStringTemplateEngine();
    private static final Yaml yaml = new Yaml(new Constructor(YamlLoader.Descriptor.class));

    private YamlLoader() {
        // private ctor
    }


    /**
     * Parse a Yaml script file
     * @param scriptFile Script file location
     * @return A new YamlLoader.Descriptor instance
     * @throws IOException
     */
    public static YamlLoader.Descriptor parseYaml(File scriptFile) throws IOException {
        return yaml.load(ResourceGroovyMethods.newReader(scriptFile));
    }

    public static Object interpolate(Object receiver, Map<String, String> data) throws IOException, ClassNotFoundException {
        if (receiver instanceof String)
            return interpolateString((String)receiver, data);

        if (receiver instanceof Map<?,?>)
            return interpolateMap((Map)receiver, data);

        return receiver;
    }

    public static Map<String, String> interpolateMap(Map receiver, Map<String, String> data) throws IOException, ClassNotFoundException {
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

    public static List interpolateList(List receiver, Map<String, String> data) throws IOException, ClassNotFoundException {
        List interpolatedValues = new ArrayList<>();

        // Iterate through elements
        for(Object value : receiver) {
            interpolatedValues.add(interpolate(value, data));
        }

        return interpolatedValues;
    }

    public static String interpolateString(String receiver, Map<String, String> data) throws IOException, ClassNotFoundException {
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

        private List<YamlScmDescriptor> scm;

        public List<YamlInvDescriptor> getInv() {
            return inv;
        }

        public void setInv(List<YamlInvDescriptor> inv) {
            this.inv = inv;
        }

        public List<YamlScmDescriptor> getScm() {
            return scm;
        }

        public void setScm(List<YamlScmDescriptor> scm) {
            this.scm = scm;
        }
    }
}
