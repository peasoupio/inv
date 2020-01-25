package io.peasoup.inv.scm

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.peasoup.inv.Main

@CompileStatic
class ScmDescriptor {

    static final Map<String, String> env =  System.getenv()
    static final List<String> env2 =  System.getenv().collect { "${it.key}=${it.value}".toString() }

    static Integer DefaultTimeout = 30000
    static String DefaultEntry = "inv.groovy"

    final HookDescriptor hooks = new HookDescriptor()
    final AskDescriptor ask = new AskDescriptor()

    private File parametersFile = null
    private Map<String, Object> parametersProperties = [:]

    ScmDescriptor(File parametersFile = null) {
        this.parametersFile = parametersFile
    }

    String name
    def name(String value) { this.name = value }

    File path = Main.currentHome
    def path(String value) {
        if (!value)
            return

        def rawFile = new File(value)
        if (Main.currentHome != Main.DEFAULT_HOME)
            this.path = new File(Main.currentHome, value)
        else
            this.path = rawFile
    }

    String src
    void src(String value) { this.src = value }

    Collection<String> entry = [DefaultEntry]
    def entry(String value) { this.entry = value.split().findAll {it } }

    Integer timeout = DefaultTimeout
    def timeout(Integer value) { this.timeout = value }

    def hooks(Closure hooksBody) {
        assert hooksBody, "Hook's body is required"

        hooksBody.resolveStrategy = Closure.DELEGATE_ONLY
        hooksBody.delegate = hooks
        hooksBody()
    }

    def ask(Closure askBody) {
        assert askBody, "Ask's body is required"

        askBody.resolveStrategy = Closure.DELEGATE_ONLY
        askBody.delegate = ask
        askBody()
    }

    //@Override
    def propertyMissing(String propertyName) {
        // Loading parameters only when need - since name is not available at ctor
        if (parametersFile && parametersFile.exists() && parametersProperties == null)
            loadParametersProperties()

        if (!parametersProperties[propertyName])
            return '\${' + propertyName + '}'

        return parametersProperties[propertyName]
    }

    private void loadParametersProperties() {
        Map<String, Map> parameters = new JsonSlurper().parseText(parametersFile.text) as Map<String, Map>
        parametersProperties.putAll(parameters[name])
    }

    class HookDescriptor {

        String init
        def init(String value) { this.init = value }

        String update
        def update(String value) { this.update = value }
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

            if (options.required && options.required instanceof Boolean)
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
