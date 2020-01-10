package io.peasoup.inv.scm

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

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

    File path
    def path(String value) { this.path = new File(value) }

    String src
    void src(String value) { this.src = value }

    Collection<String> entry = [DefaultEntry]
    def entry(String value) { this.entry = value.split().findAll {it } }

    Integer timeout = DefaultTimeout
    def timeout(Integer value) { this.timeout = value }

    def hooks(Closure hooksClosure) {
        assert hooksClosure

        hooksClosure.resolveStrategy = Closure.DELEGATE_ONLY
        hooksClosure.delegate = hooks
        hooksClosure()
    }

    def ask(Closure askClosure) {
        assert askClosure

        askClosure.resolveStrategy = Closure.DELEGATE_ONLY
        askClosure.delegate = ask
        askClosure()
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
            assert name
            assert usage

            def parameter = new AskParameter(
                    name: name,
                    usage: usage)

            if (options.defaultValue && options.defaultValue instanceof CharSequence)
                parameter.defaultValue = options.defaultValue as String

            if (options.staticValues && options.staticValues instanceof Collection<String>)
                parameter.staticValues = options.staticValues as List<String>

            if (options.commandValues && options.commandValues instanceof CharSequence)
                parameter.commandValues = options.commandValues as String

            if (options.commandFilter && options.commandFilter instanceof CharSequence)
                parameter.commandFilter = options.commandFilter as String

            parameters << parameter
        }
    }

    static class AskParameter {
        String name
        String usage
        String defaultValue
        Boolean required
        List<String> staticValues
        String commandValues
        String commandFilter // TODO Should filter also accepts a closure ?
    }
}
