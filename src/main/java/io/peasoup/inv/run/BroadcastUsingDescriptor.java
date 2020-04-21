package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class BroadcastUsingDescriptor {
    private Object id;
    private String markdown;
    private Closure ready;

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
    public void id(Map id) {
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

        this.markdown = markdown;
    }

    /**
     * Event raised when broadcast is ready during the running cycle.
     * <p>
     * A return value is expected if something is meant to be shared to other requirements.
     * <p>
     * Otherwise, "null" will be shared.
     *
     * @param readyBody the closure body receiving the ready event
     */
    public void ready(Closure readyBody) {
        this.ready = readyBody;
    }

    public Object getId() {
        return id;
    }

    public String getMarkdown() {
        return markdown;
    }

    public Closure getReady() {
        return ready;
    }


}
