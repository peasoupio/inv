package io.peasoup.inv.scm;

import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import io.peasoup.inv.Home;
import io.peasoup.inv.run.Logger;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("rawtypes")
public class ScmDescriptor {

    private static final Map<String, String> env = System.getenv();
    private static final List<String> set = env.entrySet().stream()
            .map(e -> e.getKey() + ":" + e.getValue() )
            .collect(Collectors.toList());

    private static final Integer DEFAULT_TIMEOUT = 30000;
    private static final String DEFAULT_ENTRY = "inv.groovy";

    private final HookDescriptor hooks = new HookDescriptor();
    private final AskDescriptor ask = new AskDescriptor();
    private final File parametersFile;
    private final Map<String, Object> parametersProperties;

    private String name;
    private File path = Home.getCurrent();
    private String src;
    private String[] entry = new String[]{DEFAULT_ENTRY};
    private Integer timeout = DEFAULT_TIMEOUT;

    public ScmDescriptor(File parametersFile) {
        this.parametersProperties = new LinkedHashMap<>();

        this.parametersFile = parametersFile;
    }

    public ScmDescriptor() {
        this(null);
    }

    public void name(String value) {
        if (StringUtils.isEmpty(value))
            throw new IllegalArgumentException("value");

        this.name = value;
    }

    public void path(String value) {
        if (StringUtils.isEmpty(value))
            throw new IllegalArgumentException("value");

        File filePath = new File(value);
        if (filePath.isAbsolute())
            this.path = filePath;
        else
            this.path = new File(Home.getCurrent(), value);
    }

    public void src(String value) {
        if (StringUtils.isEmpty(value))
            throw new IllegalArgumentException("value");

        this.src = value;
    }

    public void entry(String value) {
        if (StringUtils.isEmpty(value))
            throw new IllegalArgumentException("value");

        this.entry = Arrays.stream(value.split("\\r?\\n"))
                .filter(s -> (s != null && s.length() > 0))
                .toArray(String[]::new);
    }

    public void timeout(int value) {
        if (value < 0)
            throw new IllegalStateException("value cannot be zero or negative.");

        this.timeout = value;
    }

    public void hooks(@DelegatesTo(ScmDescriptor.HookDescriptor.class) Closure hooksBody) {
        if (hooksBody == null)
            throw new IllegalArgumentException("hooksBody");

        hooksBody.setResolveStrategy(Closure.DELEGATE_FIRST);
        hooksBody.setDelegate(hooks);
        hooksBody.call();
    }

    public void ask(@DelegatesTo(ScmDescriptor.AskDescriptor.class) Closure askBody) {
        if (askBody == null)
            throw new IllegalArgumentException("askBody");

        askBody.setResolveStrategy(Closure.DELEGATE_FIRST);
        askBody.setDelegate(ask);
        askBody.call();
    }

    public Object propertyMissing(final String propertyName) {
        // Loading parameters only when need - since name is not available at ctor
        loadParametersProperties();

        // Check if a value is defined in the parameter file
        Object fromParamFile = parametersProperties.get(propertyName);
        if (fromParamFile != null) return fromParamFile;

        // If not, try to use the default value
        Optional<AskParameter> fromParamDefault = ask.getParameters().stream()
                .filter(p -> p.name.equals(propertyName))
                .findFirst();

        if (fromParamDefault.isPresent()){
            String defaultValue = fromParamDefault.get().getDefaultValue();
            if (StringUtils.isNotEmpty(defaultValue))
                return defaultValue;
        }

        // Otherwise, print the propertyName
        return "${" + propertyName + "}";
    }

    @SuppressWarnings("unchecked")
    private void loadParametersProperties() {
        if (parametersFile != null && parametersFile.exists()) {

            // Try to get parameters
            Map<String, Map> parameters = null;
            try {
                parameters = (Map<String, Map>) new JsonSlurper().parseText(ResourceGroovyMethods.getText(parametersFile));
            } catch (IOException e) {
                Logger.error(e);
            }
            if (parameters == null)
                return;

            // Get parameters for this SCM only
            if (parameters.containsKey(name)) {
                parametersProperties.clear();
                parametersProperties.putAll(parameters.get(name));
            }
        }

    }

    public static Map<String, String> getEnv() {
        return env;
    }

    public static List<String> getSet() {
        return set;
    }

    public File getParametersFile() {
        return parametersFile;
    }

    public final HookDescriptor getHooks() {
        return hooks;
    }

    public final AskDescriptor getAsk() {
        return ask;
    }

    public String getName() {
        return name;
    }

    public File getPath() {
        return path;
    }

    public String getSrc() {
        return src;
    }

    public String[] getEntry() {
        return entry;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public static class HookDescriptor {
        /**
         * Indicates how the init (or initialization) phase should behave.
         * It is similar to 'git clone', 'svn checkout', 'tf get', etc.
         *
         * @param value the Shell Script (Sh) commands
         */
        public void init(String value) {
            this.init = value;
        }

        /**
         * Indicates how pull should behave
         *
         * @param value the Shell Script (Sh) commands
         */
        public void pull(String value) {
            this.pull = value;
        }

        /**
         * Indicates how to push changes to the remote source code manager.
         *
         * @param value the Shell Script (Sh) commands
         */
        public void push(String value) {
            this.push = value;
        }

        /**
         * Indicates how to retrieve the current version. <br/>
         * Important: Only the first returned line is used.
         *
         * @param value the Shell Script (Sh) commands
         */
        public void version(String value) {
            this.version = value;
        }

        /**
         * @deprecated Check 'pull' instead. Will be removed in 2021.
         * @param value the Shell Script (Sh) commands
         */
        @Deprecated
        public void update(String value) {
            Logger.warn("scm.update() is deprecated. Use scm.pull() instead.");
            pull(value);
        }

        public String getInit() {
            return init;
        }

        public void setInit(String init) {
            this.init = init;
        }

        public String getPull() {
            return pull;
        }

        public void setPull(String pull) {
            this.pull = pull;
        }

        public String getPush() {
            return push;
        }

        public void setPush(String push) {
            this.push = push;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        private String init;
        private String pull;
        private String push;
        private String version;
    }

    public static class AskDescriptor {

        private final List<AskParameter> parameters;

        public AskDescriptor() {
            parameters = new ArrayList<>();
        }

        public void parameter(String name, String usage, Map options) throws ScmHandler.SCMOptionRequiredException {
            if (StringUtils.isEmpty(name))
                throw new ScmHandler.SCMOptionRequiredException("ash/parameter/name");

            if (StringUtils.isEmpty(usage))
                throw new ScmHandler.SCMOptionRequiredException("ash/parameter/usage");

            AskParameter parameter = new AskParameter(name, usage);

            if (options == null)
                return;

            if (options.containsKey("defaultValue") && options.get("defaultValue") instanceof CharSequence)
                parameter.defaultValue = String.valueOf(options.get("defaultValue"));

            if (options.containsKey("required") && options.get("required") instanceof Boolean)
                parameter.required = (boolean)options.get("required");

            if (options.containsKey("values") && options.get("values") instanceof Collection<?>)
                //noinspection unchecked
                parameter.values = (Collection<String>) options.get("values");

            if (options.containsKey("command") && options.get("command") instanceof CharSequence)
                parameter.command = String.valueOf(options.get("command"));

            if (options.containsKey("filter") && options.get("filter") instanceof Closure)
                //noinspection unchecked
                parameter.filter = (Closure<String>)options.get("filter");

            if (options.containsKey("filterRegex") && options.get("filterRegex") instanceof CharSequence)
                parameter.filterRegex = String.valueOf(options.get("filterRegex"));

            parameters.add(parameter);
        }

        public void parameter(String name, String usage) throws ScmHandler.SCMOptionRequiredException {
            parameter(name, usage, null);
        }

        public List<AskParameter> getParameters() {
            return parameters;
        }
    }

    public static class AskParameter {
        private final String name;
        private final String usage;

        private String defaultValue;
        private Boolean required = true;
        private Collection<String> values;
        private String command;
        private Closure<String> filter;
        private String filterRegex;

        protected AskParameter(String name, String usage) {
            this.name = name;
            this.usage = usage;
        }

        public String getName() {
            return name;
        }

        public String getUsage() {
            return usage;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public Boolean getRequired() {
            return required;
        }

        public Collection<String> getValues() {
            return values;
        }

        public String getCommand() {
            return command;
        }

        public Closure<String> getFilter() {
            return filter;
        }

        public String getFilterRegex() {
            return filterRegex;
        }


    }
}