package io.peasoup.inv.run;

import groovy.lang.Closure;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

@Getter
public class BroadcastUsingDescriptor {
    private Object id;
    private String markdown;
    private boolean delayed;
    private Closure<Object> global;
    private Closure<Object> dynamic;


    /**
     * Defines the broadcast id from a generic object.
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
     * Defines the broadcast id from a Map object.
     *
     * @param id the map id
     */
    public void id(Map<String, Object> id) {
        this.id = id;
    }

    /**
     * Defines the markdown documentation for this broadcast statement
     * @param markdown the markdown string
     */
    public void markdown(String markdown) {
        if (StringUtils.isEmpty(markdown)) {
            throw new IllegalArgumentException("Markdown is required");
        }

        this.markdown = markdown.trim();
    }

    /**
     * Indicates whether or not the "ready" closure is evaluated right now
     * or only when required.
     * By default, the value is false.
     * @param delayed True if delayed, otherwise false.
     */
    public void delayed(boolean delayed) {
        this.delayed = delayed;
    }

    /**
     * Event raised when broadcast is ready to get the global response.
     * <p>
     * A return value is expected if something is meant to be shared globally
     * and does not depend on the caller information.
     * <p>
     * Otherwise, "null" will be shared.
     *
     * @param globalBody the closure body defining the global event
     */
    public void global(Closure<Object> globalBody) {
        this.global = globalBody;
    }


    /**
     * Event raised when broadcast is ready to get the global response.
     * <p>
     * A return value is expected if something is meant to be shared using
     * caller information, such as its name, path, etc.
     * <p>
     * You could also use its broadcast and require function to manipulate
     * the caller requirements and broadcasts.
     * <p>
     * Otherwise, "null" will be shared.
     *
     * @param dynamicBody the closure body defining the dynamic event
     */
    public void dynamic(Closure<Object> dynamicBody) {
        this.dynamic = dynamicBody;
    }
}
