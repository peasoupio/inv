package io.peasoup.inv.loader;

import groovy.lang.Closure;
import groovy.util.DelegatingScript;
import io.peasoup.inv.run.Inv;
import io.peasoup.inv.run.Logger;
import org.apache.commons.lang.NotImplementedException;

public class LazyYamlClosure extends Closure {

    private static final GroovyLoader GROOVYLOADER = new GroovyLoader(DelegatingScript.class.getName());

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
    public Object invokeMethod(String method, Object arguments) {
        throw new NotImplementedException();
    }

    @Override
    public Object getProperty(String property) {
        throw new NotImplementedException();
    }

    @Override
    public void setProperty(String property, Object newValue) {
        throw new NotImplementedException();
    }

    @Override
    public Object call() {
        return this.getCodeClosure().call();
    }

    @Override
    public Object call(Object arguments) {
        throw new NotImplementedException();
    }

    @Override
    public Object call(Object... args) {
        throw new NotImplementedException();
    }

    public Object doCall(Object... args) {
        throw new NotImplementedException();
    }

    @Override
    public Object getDelegate() {
        return this.getCodeClosure().getDelegate();
    }

    @Override
    public void setDelegate(Object delegate) {
        this.getCodeClosure().setDelegate(delegate);
    }

    @Override
    public Class[] getParameterTypes() {
        return this.getCodeClosure().getParameterTypes();
    }

    @Override
    public int getMaximumNumberOfParameters() {
        return this.getCodeClosure().getMaximumNumberOfParameters();
    }

    @Override
    public void run() {
        this.getCodeClosure().run();
    }

    @Override
    public Object clone() {
        throw new NotImplementedException();
    }

    @Override
    public int hashCode() {
        return this.getCodeClosure().hashCode();
    }

    @Override
    public boolean equals(Object arg0) {
        return this.getCodeClosure().equals(arg0);
    }

    @Override
    public String toString() {
        return this.getCodeClosure().toString();
    }

    @Override
    public Closure curry(Object argument) {
        throw new NotImplementedException();
    }

    @Override
    public Closure curry(Object... arguments) {
        throw new NotImplementedException();
    }

    @Override
    public void setResolveStrategy(int resolveStrategy) {
        this.getCodeClosure().setResolveStrategy(resolveStrategy);
    }

    @Override
    public int getResolveStrategy() {
        return this.getCodeClosure().getResolveStrategy();
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
                Class closureScript = GROOVYLOADER.parseClassText("def export = { " + codeBlock + " }; return export");

                try {
                    DelegatingScript script = (DelegatingScript) closureScript.getDeclaredConstructor().newInstance();
                    script.setDelegate(owner.getDelegate());
                    codeClosure = (Closure)script.run();
                } catch (Exception e) {
                    Logger.error(e);
                }
            }
        }

        if (codeClosure == null)
            throw new IllegalStateException("CodeClosure cannot be null");

        return codeClosure;
    }
}
