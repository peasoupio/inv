package io.peasoup.inv.scm

import groovy.json.JsonSlurper
import org.codehaus.groovy.control.CompilerConfiguration

class ScmDescriptor {

    static Integer DefaultTimeout = 30000
    static String DefaultEntry = "inv.groovy"

    private final DelegatingScript script
    private final def delegate = new Expando()


    ScmDescriptor(BufferedReader scriptReader) {
        // Configure the GroovyShell and pass the compiler configuration.
        def compilerConfiguration = new CompilerConfiguration()
        compilerConfiguration.scriptBaseClass = DelegatingScript.class.name
        def shell = new GroovyShell(this.class.classLoader, new Binding(), compilerConfiguration)

        script = shell.parse(scriptReader) as DelegatingScript
    }

    Map<String, MainDescriptor> scms(File externalProperties = null) {

        delegate.metaClass.methodMissing = { String methodName, args ->

            if (externalProperties)
                delegate[methodName] = new MainDescriptor(methodName, externalProperties)
            else
                delegate[methodName] = new MainDescriptor(methodName)

            if (args.size() != 1 || !(args[0] instanceof Closure))
                return null

            args[0].delegate = delegate[methodName]
            args[0]()
        }

        script.setDelegate(delegate)
        script.run()

        return delegate.properties
    }

    static class MainDescriptor {

        static final Map<String, String> env =  System.getenv()

        final String name
        final HookDescriptor hooks = new HookDescriptor()
        final AskDescriptor ask = new AskDescriptor()

        private File parametersFile

        MainDescriptor(String name, File parametersFile = null) {
            assert name

            this.name = name
            this.parametersFile = parametersFile
        }

        File path
        def path(String value) { this.path = new File(value) }

        String src
        def src(String value) { this.src = value }

        String entry = DefaultEntry
        def entry(String value) { this.entry = value }

        Integer timeout = DefaultTimeout
        def timeout(Integer value) { this.timeout = value }

        def hooks(Closure hooksClosure) {
            assert hooksClosure

            // Make sure we're calling only safe properties
            def safeProperties = [:]
            safeProperties << this.properties
            safeProperties.remove("hooks")
            safeProperties.remove("ask")
            safeProperties.remove("class")

            if (parametersFile && parametersFile.exists()) {
                Map parameters = new JsonSlurper().parseText(parametersFile.text) as Map
                safeProperties += parameters[name]
            }

            // Bind missing properties with our main ones
            hooks.metaClass.propertyMissing = { String propertyName ->
                if (safeProperties[propertyName])
                    return safeProperties[propertyName]

                return '\${' + propertyName + '}'
            }

            // Call using ONLY OUR DELEGATE.
            // Otherwise, it will inherits from all the parents - including the unsafe ones
            hooksClosure.resolveStrategy = Closure.DELEGATE_ONLY
            hooksClosure.delegate = hooks
            hooksClosure()
        }

        def ask(Closure askClosure) {
            assert askClosure

            // Make sure we're calling only safe properties
            def safeProperties = [:]
            safeProperties << this.properties
            safeProperties.remove("hooks")
            safeProperties.remove("class")

            // Bind missing properties with our main ones
            ask.metaClass.propertyMissing = { String propertyName ->
                safeProperties[propertyName]
            }

            askClosure.resolveStrategy = Closure.DELEGATE_ONLY
            askClosure.delegate = ask
            askClosure()
        }
    }

    static class HookDescriptor {

        String init
        def init(String value) { this.init = value }

        String update
        def update(String value) { this.update = value }

    }

    static class AskDescriptor {

        List<AskParameter> parameters = []

        def parameter(String name, String usage, String defaultValue = "", def values = null, String filter = null) {

            assert name
            assert usage

            parameters << new AskParameter(
                name: name,
                usage: usage,
                defaultValue: defaultValue,
                commandValues: (values != null && values instanceof CharSequence)? values as String : null,
                staticValues: (values != null && values instanceof Collection<String>)? values as List<String> : [],
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
        String filter
    }
}
