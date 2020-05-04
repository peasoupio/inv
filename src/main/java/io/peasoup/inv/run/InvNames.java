package io.peasoup.inv.run;

@SuppressWarnings("unused")
public class InvNames {
    private InvNames() {

    }

    public Object propertyMissing(String propertyName) {
        return new StatementDescriptor(propertyName);
    }

    public Object methodMissing(String methodName, Object args) {
        Object name = ((Object[])args)[0];
        return new StatementDescriptor(methodName).call(name);
    }

    public static final InvNames Instance = new InvNames();
}
