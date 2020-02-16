package io.peasoup.inv.security

import groovy.transform.TypeChecked
import io.peasoup.inv.run.Logger
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.control.customizers.SecureASTCustomizer
import org.codehaus.groovy.control.messages.ExceptionMessage

class CommonLoader {

    private static boolean systemSecureModeEnabled = false
    /**¸
     * Determine system-wide secure mode preference
     */
    static void enableSecureMode() {
        systemSecureModeEnabled = true
    }

    private final boolean secureMode = false
    private final GroovyClassLoader generalClassLoader
    private final GroovyClassLoader securedClassLoader

    /**
     * Create a common loader using system-wide secure mode preference
     */
    CommonLoader() {
        this(systemSecureModeEnabled)
    }

    /**
     * Create a common loader
     * @param secureMode Determines if using secure mode or not
     */
    CommonLoader(boolean secureMode) {

        final CompilerConfiguration compilerConfiguration = new CompilerConfiguration().with {

            SecureASTCustomizer secureASTCustomizer = new SecureASTCustomizer().with {
                packageAllowed = false
                indirectImportCheckEnabled = true

                return delegate
            }

            ASTTransformationCustomizer astTransformationCustomizer = new ASTTransformationCustomizer(
                    TypeChecked,
                    extensions: [SecuredTypeChecked.class.canonicalName])

            addCompilationCustomizers(astTransformationCustomizer )
            addCompilationCustomizers(secureASTCustomizer)

            return delegate
        }

        this.secureMode = secureMode
        this.generalClassLoader = new GroovyClassLoader()
        this.securedClassLoader = new GroovyClassLoader(Thread.currentThread().getContextClassLoader(), compilerConfiguration)
    }

    Script parseClass(File file) throws CompilationFailedException {

        if (!secureMode)
            return generalClassLoader.parseClass(file).newInstance() as Script

        try {
            return securedClassLoader.parseClass(file).newInstance() as Script
        } catch (MultipleCompilationErrorsException ex) {
            if (hasFatalException(ex))
                return null
            else
                return generalClassLoader.parseClass(file).newInstance() as Script
        }
    }

    Script parseClass(String text) throws CompilationFailedException {

        if (!secureMode)
            return generalClassLoader.parseClass(text).newInstance() as Script

        try {
            securedClassLoader.parseClass(text).newInstance() as Script
        } catch(MultipleCompilationErrorsException ex) {
            if (hasFatalException(ex))
                return null
            else
                return generalClassLoader.parseClass(text).newInstance() as Script
        }
    }

    Script parseClass(String text, String fileName) throws CompilationFailedException {

        if (!secureMode)
            return generalClassLoader.parseClass(text, fileName).newInstance() as Script

        try {
            securedClassLoader.parseClass(text, fileName).newInstance() as Script
        } catch(MultipleCompilationErrorsException ex) {
            if (hasFatalException(ex))
                return null
            else
                return generalClassLoader.parseClass(text, fileName).newInstance() as Script
        }
    }

    private boolean hasFatalException(MultipleCompilationErrorsException ex) {
        ExceptionMessage notAllowed =  ex.collector.errors.find {it.cause instanceof MethodCallNotAllowedException }
        if (notAllowed) {
            Logger.error(notAllowed.cause)
            return true
        }

        ExceptionMessage notSecure =  ex.collector.errors.find {it.cause instanceof SecurityException }
        if (notSecure) {
            Logger.error(notSecure.cause)
            return true
        }

        return false
    }

    static class MethodCallNotAllowedException extends Exception {
        MethodCallNotAllowedException(ASTNode expr) {
            super("Method call is not allowed: ${expr.getText()}")
        }
    }


}

