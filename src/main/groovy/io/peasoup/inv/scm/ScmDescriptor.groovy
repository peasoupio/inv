package io.peasoup.inv.scm

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.run.Logger

@CompileStatic
class ScmDescriptor {

    static final Map<String, String> env =  System.getenv()
    static final List<String> set =  System.getenv().collect { "${it.key}=${it.value}".toString() }

    static Integer DefaultTimeout = 30000
    static String DefaultEntry = "inv.groovy"

    final HookDescriptor hooks = new HookDescriptor()
    final AskDescriptor ask = new AskDescriptor()

    final File parametersFile
    private final Map<String, Object> parametersProperties = [:]

    ScmDescriptor(File parametersFile = null) {
        this.parametersFile = parametersFile
    }

    String name
    def name(String value) { this.name = value }

    File path = Home.getCurrent()
    def path(String value) {
        if (!value)
            return

        File filePath = new File(value)
        if (filePath.isAbsolute())
            this.path = filePath
        else
            this.path = new File(Home.getCurrent(), value)
    }

    String src
    void src(String value) { this.src = value }

    Collection<String> entry = [DefaultEntry]
    def entry(String value) { this.entry = value.split().findAll {it } }

    Integer timeout = DefaultTimeout
    def timeout(Integer value) { this.timeout = value }

    def hooks(@DelegatesTo(HookDescriptor) Closure hooksBody) {
        assert hooksBody, "Hook's body is required"

        hooksBody.resolveStrategy = Closure.DELEGATE_ONLY
        hooksBody.delegate = hooks
        hooksBody()
    }

    def ask(@DelegatesTo(AskDescriptor) Closure askBody) {
        assert askBody, "Ask's body is required"

        askBody.resolveStrategy = Closure.DELEGATE_ONLY
        askBody.delegate = ask
        askBody()
    }

    //@Override
    Object propertyMissing(String propertyName) {
        // Loading parameters only when need - since name is not available at ctor
        loadParametersProperties()

        // Check if a value is defined in the parameter file
        def fromParamFile = parametersProperties[propertyName]
        if (fromParamFile)
            return fromParamFile

        // If not, try to use the default value
        def fromParamDefault = ask.parameters.find { it.name == propertyName }
        if (fromParamDefault && fromParamDefault.defaultValue)
            return fromParamDefault.defaultValue

        // Otherwise, print the propertyName
        return '\${' + propertyName + '}'
    }

    private void loadParametersProperties() {
        if (parametersFile && parametersFile.exists()) {
            Map<String, Map> parameters = new JsonSlurper().parseText(parametersFile.text) as Map<String, Map>

            // Get parameters for this SCM only
            if (parameters[name])
                parametersProperties.putAll(parameters[name])
        }
    }

    class HookDescriptor {

        String init
        /**
         * Indicates how the init (or initialization) phase should behave.
         * It is similar to 'git clone', 'svn checkout', 'tf get', etc.
         * @param value the Shell Script (Sh) commands
         */
        void init(String value) { this.init = value }

        String pull
        /**
         * Indicates how pull should behave
         * @param value the Shell Script (Sh) commands
         */
        void pull(String value) {this.pull = value }

        String push
        /**
         * Indicates how to push changes to the remote source code manager.
         * @param value the Shell Script (Sh) commands
         */
        void push(String value) {this.push = value }

        String version
        /**
         * Indicates how to retrieve the current version. <br/>
         * Important: Only the first returned line is used.
         * @param value the Shell Script (Sh) commands
         */
        void version(String value) {this.version = value }

        /**
         * DEPRECATED. See 'pull' instead
         * @param value
         * @return
         */
        @Deprecated
        def update(String value) {
            Logger.warn("scm.update() is deprecated. Use scm.pull() instead.")
            pull(value)
        }
    }

    class AskDescriptor {

        List<AskParameter> parameters = []

        def parameter(String name, String usage, Map options = [:]) {
            if (!name)
                throw new ScmHandler.SCMOptionRequiredException("ash/parameter/name")

            if (!usage)
                throw new ScmHandler.SCMOptionRequiredException("ash/parameter/usage")

            def parameter = new AskParameter(
                    name: name,
                    usage: usage)

            if (options.defaultValue && options.defaultValue instanceof CharSequence)
                parameter.defaultValue = options.defaultValue as String

            if (options.required != null && options.required instanceof Boolean)
                parameter.required = options.required as Boolean

            if (options.values && options.values instanceof Collection<String>)
                parameter.values = options.values as List<String>

            if (options.command && options.command instanceof CharSequence)
                parameter.command = options.command as String

            if (options.filter && options.filter instanceof Closure)
                parameter.filter = options.filter as Closure

            if (options.filterRegex && options.filterRegex instanceof CharSequence)
                parameter.filterRegex = options.filterRegex as String

            parameters << parameter
        }
    }

    static class AskParameter {
        String name
        String usage
        String defaultValue
        Boolean required = true
        List<String> values
        String command
        Closure<String> filter
        String filterRegex

        protected AskParameter() {

        }
    }
}
