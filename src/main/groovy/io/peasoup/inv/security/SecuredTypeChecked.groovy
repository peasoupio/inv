package io.peasoup.inv.security

import io.peasoup.inv.run.InvDescriptor
import io.peasoup.inv.run.InvHandler
import io.peasoup.inv.run.InvNames
import io.peasoup.inv.run.StatementDescriptor
import io.peasoup.inv.scm.ScmDescriptor
import io.peasoup.inv.scm.ScmHandler
import org.codehaus.groovy.transform.stc.GroovyTypeCheckingExtensionSupport

class SecuredTypeChecked extends GroovyTypeCheckingExtensionSupport.TypeCheckingDSL {

    def blacklistedClasses = [
            'java.lang.System',
            'java.lang.Thread',
            'groovy.util.Eval'
    ]

    Map<String, Class> knownHandler = [
            'inv': InvHandler,
            'scm': ScmHandler
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

        methodNotFound { receiver, name, argList, argTypes, call ->
            if (name in knownHandler)
                return makeDynamic(call, classNodeFor(knownHandler[name]))

            if (receiver == classNodeFor(InvNames))
                return makeDynamic(call, classNodeFor(InvNames))
        }

        unresolvedProperty { pexp ->
            if (getType(pexp.objectExpression)==classNodeFor(InvNames)) {
                handled = true
                return storeType(pexp,classNodeFor(StatementDescriptor))
            }
        }

        unresolvedVariable { var ->
            if (var.name == 'pwd') {
                handled = true
                return storeType(var, classNodeFor(String))
            }

            if (var.name == '$0') {
                handled = true
                return storeType(var, classNodeFor(String))
            }
        }

    }
}


