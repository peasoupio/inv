package io.peasoup.inv.run;

import groovy.lang.Closure;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.LinkedHashMap;

@SuppressWarnings({"rawtypes", "unchecked"})
@Getter
@Setter
public class RequireStatement implements Statement {
    public static final RequireProcessor REQUIRE_PROCESSOR = new RequireProcessor();
    private Object id;
    private String name;
    private String markdown;
    private Boolean optional = false;
    private Boolean dynamic = false;
    private Inv inv;
    private String into;
    private Closure<Object> resolved;
    private Closure<Object> unresolved;
    protected StatementStatus state = StatementStatus.NOT_PROCESSED;

    public Processor getProcessor() {
        return REQUIRE_PROCESSOR;
    }

    /**
     * Calls the "resolved" callback method, if defined, with a response
     * @param broadcastResponse The response
     */
    public void resolve(BroadcastResponse broadcastResponse) {
        if (broadcastResponse == null)
            throw new IllegalArgumentException("broadcastResponse");

        String intoVariable;

        if (StringUtils.isNotEmpty(this.getInto()))
            intoVariable = this.getInto();
        else {
            intoVariable = getDefaultIntoVariableName();
        }

        Object shell = resolveShell( broadcastResponse);

        this.getInv().addProperty(
                intoVariable,
                shell);

        // Sends message to resolved (if defined)
        if (this.getResolved() != null) {
            this.getResolved().setDelegate(shell);
            this.getResolved().run();
        }

        // Check if NV would have dumped something
        this.getInv().dumpDelegate();
    }

    /**
     * Calls the "unresolved" callback, if defined.
     */
    public void unresolve() {
        if (this.getUnresolved() == null)
            return;

        LinkedHashMap<String, Object> map = new LinkedHashMap<>(3);
        map.put("name", this.getName());
        map.put("id", this.getId());
        map.put("owner", this.getInv().getName());
        this.getUnresolved().call(map);
    }

    /**
     *  Gets the default into variable name.
     *  It consists of the first letter of "name", in lowercase, the rest as it is, preceded by a '$'.
     * @return The default into variable name
     */
    private String getDefaultIntoVariableName() {

        char[] c = this.getName().toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        return "$" + new String(c);
    }

    /**
     * Gets the shell value.
     * Global is used if "dynamic" is set to false, otherwise the dynamic response is taken.
     *
     * @param broadcastResponse The broadcast response
     * @return If no response (null) is resolved, returns a new BroadcastNullResponseNonMetaclass.
     *         If dynamic is true, returns a new BroadcastDynamicResponseMetaClass.
     *         Otherwise, returns a new BroadcastGlobalResponseMetaClass.
     */
    private Object resolveShell(BroadcastResponse broadcastResponse) {
        if (this.getDynamic()) {
            return broadcastResponse.getDynamicResponse(this);
        } else {
            return broadcastResponse.getGlobalResponse();
        }
    }

    @Override
    public String getLabel() {
        if (Boolean.TRUE.equals(optional))
            return "[OPTIONAL] [" + getName() + "] " + DefaultGroovyMethods.toString(getId());

        return "[REQUIRE] [" + getName() + "] " + DefaultGroovyMethods.toString(getId());
    }

    @Override
    public void applyResolvedID() {
        this.id = IdResolver.resolve(this);
    }

    @Override
    public int hashCode() {
        return StatementHasher.hashcode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return StatementHasher.equals(this, obj);
    }

    @Override
    public String toString() {
        return getInv() + " => " + getLabel();
    }
}
