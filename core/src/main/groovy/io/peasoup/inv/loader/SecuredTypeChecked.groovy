package io.peasoup.inv.loader

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.peasoup.inv.repo.RepoDescriptor
import io.peasoup.inv.run.InvDescriptor
import org.codehaus.groovy.transform.stc.GroovyTypeCheckingExtensionSupport

@CompileStatic
class SecuredTypeChecked extends GroovyTypeCheckingExtensionSupport.TypeCheckingDSL {

    List<String> blacklistedClasses = [
            'java.lang.System',
            'java.lang.Thread',
            'groovy.util.Eval'
    ]

    Map<String, Class> knownDescriptor = [
            'inv': InvDescriptor,
            'repo': RepoDescriptor,
            'ask': RepoDescriptor.AskDescriptor,
            'hooks': RepoDescriptor.HookDescriptor
    ] as Map<String, Class>

    @CompileDynamic
    @Override
    Object run() {

        // https://melix.github.io/blog/2015/03/sandboxing.html


        onMethodSelection { expr, methodNode ->
            if (methodNode.declaringClass.name in blacklistedClasses)
                throw new GroovyLoader.MethodCallNotAllowedException(expr)

            if (methodNode.name in knownDescriptor) {
                handled = true
                delegatesTo(classNodeFor(knownDescriptor[methodNode.name]))
            }
        }

        beforeVisitMethod { methodNode ->
            if (methodNode in knownDescriptor) {
                handled = true
                delegatesTo(classNodeFor(knownDescriptor[methodNode]))
            }
        }

    }
}


