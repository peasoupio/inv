package io.peasoup.inv.security


import org.apache.groovy.groovysh.util.NoExitSecurityManager
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.SecureASTCustomizer

class CommonLoader extends GroovyClassLoader {

    static {
        System.setSecurityManager(new NoExitSecurityManager())
    }

    static final CompilerConfiguration compilerConfiguration = new CompilerConfiguration().with {

        SecureASTCustomizer secureASTCustomizer = new SecureASTCustomizer()
        secureASTCustomizer.with {
            packageAllowed = false
            indirectImportCheckEnabled = true

            receiversBlackList = [
                    System.class.canonicalName,
                    Thread.class.canonicalName,
                    Eval.class.canonicalName
            ]
        }

        addCompilationCustomizers(secureASTCustomizer)

        return delegate
    }

    CommonLoader() {
        super(Thread.currentThread().getContextClassLoader(), compilerConfiguration)
    }
}
