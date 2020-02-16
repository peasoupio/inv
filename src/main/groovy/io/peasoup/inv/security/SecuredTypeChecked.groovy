package io.peasoup.inv.security

import io.peasoup.inv.run.InvDescriptor
import io.peasoup.inv.scm.ScmDescriptor
import org.codehaus.groovy.transform.stc.GroovyTypeCheckingExtensionSupport

class SecuredTypeChecked extends GroovyTypeCheckingExtensionSupport.TypeCheckingDSL {

    def blacklistedClasses = [
            'java.lang.System',
            'java.lang.Thread',
            'groovy.util.Eval'
    ]

    Map<String, Class> knownDescriptor = [
            'inv': InvDescriptor,
            'scm': ScmDescriptor,
            'ask': ScmDescriptor.AskDescriptor,
            'hooks': ScmDescriptor.HookDescriptor
    ]

    @Override
    Object run() {

        onMethodSelection { expr, methodNode ->
            if (methodNode.declaringClass.name in blacklistedClasses)
                throw new CommonLoader.MethodCallNotAllowedException(expr)

            if (methodNode.name in knownDescriptor)
                return delegatesTo(classNodeFor(knownDescriptor[methodNode.name]))
        }
    }
}


