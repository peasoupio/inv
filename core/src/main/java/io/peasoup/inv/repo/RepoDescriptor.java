package io.peasoup.inv.repo;

import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import io.peasoup.inv.Logger;
import io.peasoup.inv.MissingOptionException;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("rawtypes")
public class RepoDescriptor {

    private static final Map<String, String> env = System.getenv();
    private static final List<String> windowsSet = env.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue() )
            .collect(Collectors.toList());
    private static final List<String> unixSet = env.entrySet().stream()
            .map(e -> e.getKey() + ":" + e.getValue() )
            .collect(Collectors.toList());

    public static final String DEFAULT_PATH = ".inv";
    public static final Integer DEFAULT_TIMEOUT = 30000;

    private static final String NAME_REGEX = "^[\\w-\\.]*$";
    private static final Pattern NAME_PATTERN = Pattern.compile(NAME_REGEX);

    private final File scriptFile;
    private final File parametersFile;
    private final Map<String, Object> parametersProperties;
    private final HookDescriptor hooks;
    private final AskDescriptor ask;

    private String name;
    private String path = DEFAULT_PATH;
    private String src;
    private Integer timeout = DEFAULT_TIMEOUT;

    private File repoPath;
    private File completeRepoPath;

    public RepoDescriptor(File scriptFile, File parametersFile) {
        if (scriptFile == null)
            throw new IllegalArgumentException("scriptFile");

        this.scriptFile = scriptFile;
        this.parametersFile = parametersFile;
        this.parametersProperties = new LinkedHashMap<>();
        this.hooks = new HookDescriptor();
        this.ask = new AskDescriptor();
    }

    public RepoDescriptor(File scriptFile) {
        this(scriptFile,null);
    }

    public void name(String name) {
        if (StringUtils.isEmpty(name))
            throw new IllegalArgumentException("name");

        if (!NAME_PATTERN.matcher(name).matches())
            throw new IllegalArgumentException("name must match the following regex: " + NAME_REGEX);

        this.name = name;

        generateRepoPaths();
    }

    public void path(String path) {
        if (StringUtils.isEmpty(path))
            throw new IllegalArgumentException("path");

        if (new File(path).isAbsolute())
            throw new IllegalArgumentException("path cannot be absolute");

        this.path = path;

        generateRepoPaths();
    }

    public void src(String src) {
        if (StringUtils.isEmpty(src))
            throw new IllegalArgumentException("src");

        this.src = src;
    }

    public void timeout(int timeout) {
        if (timeout < 0)
            throw new IllegalStateException("timeout cannot be zero or negative.");

        this.timeout = timeout;
    }

    public void hooks(@DelegatesTo(RepoDescriptor.HookDescriptor.class) Closure hooksBody) {
        if (hooksBody == null)
            throw new IllegalArgumentException("hooksBody");

        hooksBody.setResolveStrategy(Closure.DELEGATE_FIRST);
        hooksBody.setDelegate(hooks);
        hooksBody.run();
    }

    public void ask(@DelegatesTo(RepoDescriptor.AskDescriptor.class) Closure askBody) {
        if (askBody == null)
            throw new IllegalArgumentException("askBody");

        askBody.setResolveStrategy(Closure.DELEGATE_FIRST);
        askBody.setDelegate(ask);
        askBody.run();
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
                String paramertersText =  ResourceGroovyMethods.getText(parametersFile);
                if (StringUtils.isEmpty(paramertersText))
                    return;

                parameters = (Map<String, Map>) new JsonSlurper().parseText(paramertersText);
            } catch (IOException e) {
                Logger.error(e);
            }
            if (parameters == null)
                return;

            // Get parameters for this REPO only
            if (parameters.containsKey(name)) {
                parametersProperties.clear();
                parametersProperties.putAll(parameters.get(name));
            }
        }

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

    public String getPath() {
        return path;
    }

    public String getSrc() {
        return src;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public File getRepoPath() {
        return this.repoPath;
    }

    public File getRepoCompletePath() {
        return this.completeRepoPath;
    }

    private void generateRepoPaths() {
        this.repoPath = new File(
            scriptFile.getParentFile(),
            //".repos" +
            //File.separator +
            scriptFile.getName().split("\\.")[0] +
            "@" +
            name
        );

        this.completeRepoPath = new File(this.repoPath,
                path);
    }


    public static Map<String, String> getEnv() {
        return env;
    }

    public static List<String> getCurrentOSSet() {
        return System.getProperty("os.name").startsWith("Windows")?
                windowsSet:
                unixSet;
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

        private static final String PARAM_DEFAULT_VALUE = "defaultValue";
        private static final String PARAM_REQUIRED = "required";
        private static final String PARAM_VALUES = "values";
        private static final String PARAM_COMMAND = "command";
        private static final String PARAM_FILTER = "filter";
        private static final String PARAM_FILTER_REGEX = "filterRegex";

        private static final String HELP_LINK = "https://github.com/peasoupio/inv/wiki/REPO-groovy-Syntax";

        private final List<AskParameter> parameters;

        public AskDescriptor() {
            parameters = new ArrayList<>();
        }

        public void parameter(String name, String usage, Map options) throws MissingOptionException {
            if (StringUtils.isEmpty(name))
                throw new MissingOptionException("ask/parameter/name", HELP_LINK);

            if (!name.matches("^[a-zA-Z0-9]*$"))
                throw new IllegalArgumentException("name must be an alphanumeric only");

            if (StringUtils.isEmpty(usage))
                throw new MissingOptionException("ask/parameter/usage", HELP_LINK);

            AskParameter parameter = new AskParameter(name, usage);

            if (options == null)
                return;

            if (options.containsKey(PARAM_DEFAULT_VALUE) && options.get(PARAM_DEFAULT_VALUE) instanceof CharSequence)
                parameter.defaultValue = String.valueOf(options.get(PARAM_DEFAULT_VALUE));

            if (options.containsKey(PARAM_REQUIRED) && options.get(PARAM_REQUIRED) instanceof Boolean)
                parameter.required = (boolean)options.get(PARAM_REQUIRED);

            if (options.containsKey(PARAM_VALUES) && options.get(PARAM_VALUES) instanceof Collection<?>) {
                //noinspection unchecked
                parameter.values = (Collection<String>) options.get(PARAM_VALUES);
            }

            if (options.containsKey(PARAM_COMMAND) && options.get(PARAM_COMMAND) instanceof CharSequence)
                parameter.command = String.valueOf(options.get(PARAM_COMMAND));

            if (options.containsKey(PARAM_FILTER) && options.get(PARAM_FILTER) instanceof Closure) {
                //noinspection unchecked
                parameter.filter = (Closure<String>) options.get(PARAM_FILTER);
            }

            if (options.containsKey(PARAM_FILTER_REGEX) && options.get(PARAM_FILTER_REGEX) instanceof CharSequence)
                parameter.filterRegex = String.valueOf(options.get(PARAM_FILTER_REGEX));

            parameters.add(parameter);
        }

        public void parameter(String name, String usage) throws MissingOptionException {
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
