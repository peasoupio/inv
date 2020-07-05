package io.peasoup.inv.run.yaml;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import groovy.util.DelegatingScript;
import io.peasoup.inv.run.Inv;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.runtime.CurriedClosure;

public class LazyYamlClosure extends Closure {

    private static final CompilerConfiguration COMPILER_CONFIGURATION = new CompilerConfiguration();
    private static final GroovyShell GROOVY_SHELL;

    // Initialize GroovyShell
    static {
        COMPILER_CONFIGURATION.setScriptBaseClass(DelegatingScript.class.getName());
        GROOVY_SHELL = new GroovyShell(
                Thread.currentThread().getContextClassLoader(),
                new Binding(),
                COMPILER_CONFIGURATION);
    }

    private final transient Inv owner;
    private final transient String codeBlock;
    private final transient Object parsingLock = new Object();

    private transient Closure codeClosure; // lazy

    public LazyYamlClosure(Inv owner, String codeBlock) {
        super(owner);

        this.owner = owner;
        this.codeBlock = codeBlock;
    }

    @Override
    public Object invokeMethod(String method, Object arguments) { return getCodeClosure().invokeMethod(method, arguments); }

    @Override
    public Object getProperty(String property) {
        return getCodeClosure().getProperty(property);
    }

    @Override
    public void setProperty(String property, Object newValue) {
        getCodeClosure().setProperty(property, newValue);
    }

    @Override
    public Object call() {
        return getCodeClosure().call();
    }

    @Override
    public Object call(Object arguments) {
        return getCodeClosure().call(arguments);
    }

    @Override
    public Object call(Object... args) {
        return getCodeClosure().call(args);
    }

    public Object doCall(Object... args) {
        return this.call(args);
    }

    @Override
    public Object getDelegate() {
        return getCodeClosure().getDelegate();
    }

    @Override
    public void setDelegate(Object delegate) {
        getCodeClosure().setDelegate(delegate);
    }

    @Override
    public Class[] getParameterTypes() {
        return getCodeClosure().getParameterTypes();
    }

    @Override
    public int getMaximumNumberOfParameters() {
        return getCodeClosure().getMaximumNumberOfParameters();
    }

    @Override
    public void run() {
        getCodeClosure().run();
    }

    @Override
    @SuppressWarnings("squid:S1182")
    public Object clone() {
        return getCodeClosure().clone();
    }

    @Override
    public int hashCode() {
        return getCodeClosure().hashCode();
    }

    @Override
    public boolean equals(Object arg0) {
        return getCodeClosure().equals(arg0);
    }

    @Override
    public String toString() {
        return getCodeClosure().toString();
    }

    @Override
    public Closure curry(Object... arguments) {
        return (new CurriedClosure(this, arguments)).asWritable();
    }

    @Override
    public void setResolveStrategy(int resolveStrategy) {
        getCodeClosure().setResolveStrategy(resolveStrategy);
    }

    @Override
    public int getResolveStrategy() {
        return getCodeClosure().getResolveStrategy();
    }

    /**
     * Gets the codeClosure.
     * If not initialized, codeBlock is parsed into a Closure object
     * @return Closure representation of codeBlock
     */
    private Closure getCodeClosure() {

        if (codeClosure == null) {
            synchronized (parsingLock) {
                // Double-lock checking
                if (codeClosure != null)
                    return codeClosure;

                // Wrap codeBlock inside Closure to make sure we get a Closure object.
                // Inspired by module.export in Javascript.
                DelegatingScript script = (DelegatingScript) GROOVY_SHELL.parse(
                        "def export = { " + codeBlock + " }; return export");
                script.setDelegate(owner.getDelegate());

                codeClosure = (Closure)script.run();
            }
        }

        if (codeClosure == null)
            throw new IllegalStateException("CodeClosure cannot be null");

        return codeClosure;
    }
}
