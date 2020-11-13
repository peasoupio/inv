package io.peasoup.inv.run;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.apache.commons.lang.StringUtils;

public class RequireDescriptor {
    private final RequireStatement requireStatement;

    public RequireDescriptor(RequireStatement requireStatement) {
        if (requireStatement == null) {
            throw new IllegalArgumentException("RequireStatement is required");
        }

        this.requireStatement = requireStatement;
    }

    /**
     * Defines the require using descriptor using a RequireUsingDescriptor instance.
     *
     * @param requireUsingDescriptor RequireUsingDescriptor instance
     * @return RequireDescriptor owner reference
     */
    public RequireDescriptor using(RequireUsingDescriptor requireUsingDescriptor) {
        if (requireUsingDescriptor == null) throw new IllegalArgumentException("requireUsingDescriptor");

        if (requireUsingDescriptor.getId() != null)
            requireStatement.setId(requireUsingDescriptor.getId());

        if (requireUsingDescriptor.getDefaults() != null)
            requireStatement.setDefaults(requireUsingDescriptor.getDefaults());

        if (requireUsingDescriptor.getUnbloatable() != null)
            requireStatement.setUnbloatable(requireUsingDescriptor.getUnbloatable());

        requireStatement.setMarkdown(requireUsingDescriptor.getMarkdown());
        requireStatement.setResolved(requireUsingDescriptor.getResolved());
        requireStatement.setUnresolved(requireUsingDescriptor.getUnresolved());

        return this;
    }

    /**
     * Defines the require using descriptor using a closure.
     *
     * @param usingBody Closure representation of RequireUsingDescriptor
     * @return RequireDescriptor owner reference
     */
    public RequireDescriptor using(@DelegatesTo(RequireUsingDescriptor.class) Closure<Object> usingBody) {
        if (usingBody == null) {
            throw new IllegalArgumentException("Using body is required");
        }

        RequireUsingDescriptor requireUsingDescriptor = new RequireUsingDescriptor();

        usingBody.setResolveStrategy(Closure.DELEGATE_FIRST);
        usingBody.setDelegate(requireUsingDescriptor);
        usingBody.run();

        return using(requireUsingDescriptor);
    }

    /**
     * Defines a requirement statement for which the broadcast "response" is automatically referenced by
     * a property (below it is "$myResponse").
     * <p>
     * It is a sub-option of the require statement.
     * <p>
     * The property is available in the NEXT scope. This includes step().
     * Will work:
     * <pre>
     *     require { Something } into '$something'
     *     broadcast { Else } using {
     *         ready {
     *             $something...
     *         }
     *     }
     * </pre>
     * Will NOT work:
     * <pre>
     *     require { Something } into '$something'
     *     require { Else($something...) }
     * </pre>
     *
     * By convention, you should add "$" before the name.
     * @param variable The name of the variable
     * @return Current require descriptor
     */
    public RequireDescriptor into(String variable) {
        if (StringUtils.isEmpty(variable)) {
            throw new IllegalArgumentException("Variable is required");
        }

        requireStatement.setInto(variable);

        return this;
    }
}
