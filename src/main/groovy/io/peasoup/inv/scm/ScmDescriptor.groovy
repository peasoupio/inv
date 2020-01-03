package io.peasoup.inv.scm

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

@CompileStatic
class ScmDescriptor {

    static final Map<String, String> env =  System.getenv()

    static Integer DefaultTimeout = 30000
    static String DefaultEntry = "inv.groovy"

    final HookDescriptor hooks = new HookDescriptor()
    final AskDescriptor ask = new AskDescriptor()

    private Map<String, Object> parametersProperties = [:]

    ScmDescriptor(File parametersFile = null) {

        if (parametersFile && parametersFile.exists()) {
            Map<String, Map> parameters = new JsonSlurper().parseText(parametersFile.text) as Map<String, Map>
            parametersProperties.putAll(parameters[name])
        }
    }

    String name
    def name(String value) { this.name = value }

    File path
    def path(String value) { this.path = new File(value) }

    String src
    void src(String value) { this.src = value }

    String entry = DefaultEntry
    def entry(String value) { this.entry = value }

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
        if (!parametersProperties[propertyName])
            return '\${' + propertyName + '}'

        return parametersProperties[propertyName]
    }

    class HookDescriptor {

        String init
        def init(String value) { this.init = value }

        String update
        def update(String value) { this.update = value }
    }

    class AskDescriptor {

        List<AskParameter> parameters = []

        // TODO Maybe parameters should be gathered within a map, it could be clearer ?
        def parameter(String name, String usage, String defaultValue = "", def values = null, String filter = null) {

            assert name
            assert usage

            parameters << new AskParameter(
                    name: name,
                    usage: usage,
                    defaultValue: defaultValue,
                    commandValues: (values != null && values instanceof CharSequence)? values as String : null,
                    staticValues: (values != null && values instanceof Collection<String>)? values as List<String> : [] as List<String>,
                    filter: filter
            )
        }

    }

    static class AskParameter {
        String name
        String usage
        String defaultValue
        String commandValues
        List<String> staticValues
        String filter // TODO Should filter also accepts a closure ?
    }
}
