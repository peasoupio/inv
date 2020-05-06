package io.peasoup.inv.run;

import groovy.lang.GroovyInterceptable;
import groovy.lang.MetaClass;
import org.apache.commons.lang3.NotImplementedException;
import org.codehaus.groovy.runtime.InvokerHelper;

@SuppressWarnings("unused")
public class InvNames implements GroovyInterceptable {
    private InvNames() {

    }

    public static final InvNames Instance = new InvNames();

    @Override
    public Object invokeMethod(String methodName, Object args) {
        if ("asBoolean".equals(methodName))
            return true;

        Object name = ((Object[])args)[0];
        return new StatementDescriptor(methodName).call(name);
    }

    @Override
    public Object getProperty(String propertyName) {
        return new StatementDescriptor(propertyName);
    }

    @Override
    public MetaClass getMetaClass() {
        return InvokerHelper.getMetaClass(StatementDescriptor.class);
    }

    @Override
    public void setMetaClass(MetaClass metaClass) {
        throw new NotImplementedException("setMetaClass");
    }
}
