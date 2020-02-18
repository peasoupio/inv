package io.peasoup.inv.run;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.apache.commons.lang.StringUtils;

public class RequireDescriptor {
    private final RequireStatement requireStatement;

    public RequireDescriptor(RequireStatement requireStatement) {
        assert requireStatement != null : "RequireStatement is required";

        this.requireStatement = requireStatement;
    }

    public RequireDescriptor using(@DelegatesTo(RequireUsingDescriptor.class) Closure usingBody) {
        assert usingBody != null : "Using body is required";

        RequireUsingDescriptor requireUsingDescriptor = new RequireUsingDescriptor();

        usingBody.setResolveStrategy(Closure.DELEGATE_FIRST);
        usingBody.setDelegate(requireUsingDescriptor);
        usingBody.call();

        if (requireUsingDescriptor.getId() != null)
            requireStatement.setId(requireUsingDescriptor.getId());

        if (requireUsingDescriptor.getDefaults() != null)
            requireStatement.setDefaults(requireUsingDescriptor.getDefaults());

        if (requireUsingDescriptor.getUnbloatable() != null)
            requireStatement.setUnbloatable(requireUsingDescriptor.getUnbloatable());

        requireStatement.setResolved(requireUsingDescriptor.getResolved());
        requireStatement.setUnresolved(requireUsingDescriptor.getUnresolved());

        return this;
    }

    public RequireDescriptor into(String variable) {
        assert StringUtils.isNotEmpty(variable) : "Variable is required";

        requireStatement.setInto(variable);

        return this;
    }
}
