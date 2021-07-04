package io.peasoup.inv.run;

import groovy.lang.Closure;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

@Getter
public class RequireUsingDescriptor {
    private Object id;
    private String markdown;
    private Closure<Object> resolved;
    private Closure<Object> unresolved;
    private Boolean optional;
    private Boolean dynamic;
    private Boolean impersonate;

    /**
     * Defines the requirement id from a generic object.
     * <p>
     * NOTE : Null is authorized and is meant as an "undefined/global" id.
     * <p>
     * In this case, the network valuable name must be relevant enough.
     *
     * @param id the object id
     */
    public void id(Object id) {
        this.id = id;
    }

    /**
     * Defines the requirement id from a Map object.
     *
     * @param id the map id
     */
    public void id(Map<String, Object> id) {
        this.id = id;
    }

    /**
     * Defines the markdown documentation for this require statement
     * @param markdown the markdown string
     */
    public void markdown(String markdown) {
        if (StringUtils.isEmpty(markdown)) {
            throw new IllegalArgumentException("Markdown is required");
        }

        this.markdown = markdown.trim();
    }

    /**
     * Event raised when requirement is resolved during the running cycle.
     * <p>
     * No return value is expected.
     *
     * @param resolvedBody the closure body receiving the resolved event
     */
    public void resolved(Closure<Object> resolvedBody) {
        this.resolved = resolvedBody;
    }

    /**
     * Event raised when requirement is not resolved during the optional.
     * <p>
     * No return value is expected.
     *
     * @param unresolvedBody the closure body receiving the unresolved event
     */
    public void unresolved(Closure<Object> unresolvedBody) {
        this.unresolved = unresolvedBody;
    }

    /**
     * Defines if this requirement (network valuable) optional upon the optional cycle
     *
     * @param value the boolean value
     */
    public void optional(boolean value) {
        this.optional = value;
    }

    /**
     * Defines if this requirement gets the global or the dynamic response of the resolved broadcast.
     *
     * @param value the boolean value
     */
    public void dynamic(boolean value) {
        this.dynamic = value;
    }
}
