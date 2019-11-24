package io.peasoup.inv.scm

import org.codehaus.groovy.control.CompilerConfiguration

class ScmDescriptor {

    static Integer DefaultTimeout = 30000
    static String DefaultEntry = "inv.groovy"

    private def delegate = new Expando()


    ScmDescriptor(BufferedReader scriptReader ) {
        // Configure the GroovyShell and pass the compiler configuration.
        def compilerConfiguration = new CompilerConfiguration()
        compilerConfiguration.scriptBaseClass = DelegatingScript.class.name
        def shell = new GroovyShell(this.class.classLoader, new Binding(), compilerConfiguration)
        def script = shell.parse(scriptReader) as DelegatingScript

        dynamicDelegate()

        script.setDelegate(delegate)
        script.run()
    }

    Map<String, MainDescriptor> scms() {
        return delegate.properties
    }

    private def dynamicDelegate() {
        delegate.metaClass.methodMissing = { String methodName, args ->
            delegate[methodName] = new MainDescriptor(methodName)

            if (args.size() != 1 || !(args[0] instanceof Closure))
                return null

            args[0].delegate = delegate[methodName]
            args[0]()
        }
    }

    static class MainDescriptor {

        static final Map<String, String> env =  System.getenv()

        final String name
        final HookDescriptor hooks = new HookDescriptor()

        MainDescriptor(String name) {
            this.name = name
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
            safeProperties.remove("class")

            // Bind missing properties with our main ones
            hooks.metaClass.propertyMissing = { String propertyName ->
                safeProperties[propertyName]
            }

            // Call using ONLY OUR DELEGATE.
            // Otherwise, it will inherits from all the parents - including the unsafe ones
            hooksClosure.resolveStrategy = Closure.DELEGATE_ONLY
            hooksClosure.delegate = hooks
            hooksClosure()
        }
    }

    static class HookDescriptor {

        String init
        def init(String value) { this.init = value }

        String update
        def update(String value) { this.update = value }

    }
}
