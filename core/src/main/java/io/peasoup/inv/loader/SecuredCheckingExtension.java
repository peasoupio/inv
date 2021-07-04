package io.peasoup.inv.loader;

import io.peasoup.inv.repo.RepoDescriptor;
import io.peasoup.inv.run.InvDescriptor;
import org.apache.groovy.util.Maps;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.codehaus.groovy.transform.stc.AbstractTypeCheckingExtension;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingVisitor;

import java.util.List;
import java.util.Map;

public class SecuredCheckingExtension extends AbstractTypeCheckingExtension {

    private final List<String> blacklistedClasses = List.of(
            "java.lang.System",
            "java.lang.Thread",
            "groovy.util.Eval"
    );

    private final Map<String, Class<?>> knownDescriptor = Maps.of(
            "inv", InvDescriptor.class,
            "repo", RepoDescriptor.class,
            "ask", RepoDescriptor.AskDescriptor.class,
            "hooks", RepoDescriptor.HookDescriptor.class
    );

    public SecuredCheckingExtension(StaticTypeCheckingVisitor typeCheckingVisitor) {
        super(typeCheckingVisitor);
    }

    @Override
    public void onMethodSelection(Expression expression, MethodNode target) {

        if (checkBlacklist(target.getDeclaringClass().getName(), expression))
            return;

        if (checkDelegate(target.getName()))
            return;

        super.onMethodSelection(expression, target);
    }

    @Override
    public boolean beforeVisitMethod(MethodNode node) {
        if (checkDelegate(node.getName()))
            return true;

        return super.beforeVisitMethod(node);
    }

    private boolean checkBlacklist(String className, Expression expression) {
        if (!blacklistedClasses.contains(className))
            return false;

        throw (GroovyLoader.MethodCallNotAllowedException) StackTraceUtils.sanitize(
                new GroovyLoader.MethodCallNotAllowedException(expression));
    }

    private boolean checkDelegate(String methodName) {
        if (!knownDescriptor.containsKey(methodName))
            return false;

        super.handled = true;
        super.delegatesTo(classNodeFor(knownDescriptor.get(methodName)));

        return true;
    }
}
