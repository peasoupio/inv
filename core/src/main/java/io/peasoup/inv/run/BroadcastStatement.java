package io.peasoup.inv.run;

import groovy.lang.Closure;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

@SuppressWarnings({"rawtypes", "unchecked"})
@Getter
@Setter
public class BroadcastStatement implements Statement {
    public static final BroadcastProcessor BROADCAST_PROCESSOR = new BroadcastProcessor();

    private Object id;
    private String name;
    private String markdown;
    private boolean delayed;
    private Closure<Object> global;
    private Closure<Object> dynamic;
    private Inv inv;

    protected StatementStatus state = StatementStatus.NOT_PROCESSED;

    public Processor getProcessor() {
        return BROADCAST_PROCESSOR;
    }

    public boolean isDynamic() {
        return dynamic != null;
    }

    @Override
    public String getLabel() {
        return "[BROADCAST] [" + getName() + "] " + DefaultGroovyMethods.toString(getId());
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
