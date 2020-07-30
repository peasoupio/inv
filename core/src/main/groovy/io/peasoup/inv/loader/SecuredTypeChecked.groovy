package io.peasoup.inv.loader

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.peasoup.inv.run.InvDescriptor
import io.peasoup.inv.scm.ScmDescriptor
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
            'scm': ScmDescriptor,
            'ask': ScmDescriptor.AskDescriptor,
            'hooks': ScmDescriptor.HookDescriptor
    ] as Map<String, Class>

    @CompileDynamic
    @Override
    Object run() {

        onMethodSelection { expr, methodNode ->
            if (methodNode.declaringClass.name in blacklistedClasses)
                throw new GroovyLoader.MethodCallNotAllowedException(expr)

            if (methodNode.name in knownDescriptor)
                return delegatesTo(classNodeFor(knownDescriptor[methodNode.name]))
        }
    }
}


