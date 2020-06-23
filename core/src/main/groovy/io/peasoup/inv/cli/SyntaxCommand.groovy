package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.fs.Pattern
import io.peasoup.inv.run.Logger
import io.peasoup.inv.security.CommonLoader
import org.codehaus.groovy.control.MultipleCompilationErrorsException

@CompileStatic
class SyntaxCommand implements CliCommand {

    List<String> patterns
    String exclude

    int call() {
        assert patterns != null, 'A valid value is required for patterns'
        if (patterns.isEmpty())
            return -1

        // Handle excluding patterns
        def excludePatterns = [".runs/*"]
        if (exclude)
            excludePatterns.add(exclude)

        def commonLoader = new CommonLoader()
        def syntaxFiles = Pattern.get(patterns, excludePatterns, Home.getCurrent())
        def succeeded = true

        syntaxFiles.each {
            try {
                def result = commonLoader.compile(it)
                if (result)
                    Logger.info("[SYNTAX] startup succeeded: ${it.absolutePath}")
                else {
                    Logger.warn("[SYNTAX] startup failed: ${it.absolutePath}")
                    succeeded = false
                }
            } catch (MultipleCompilationErrorsException ex) {
                Logger.warn("[SYNTAX] ${ex.getMessage()}")
                succeeded = false
            }
        }

        return succeeded? 0 : -2
    }

    boolean rolling() {
        return false
    }
}
