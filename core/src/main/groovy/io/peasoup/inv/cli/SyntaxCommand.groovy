package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.fs.Pattern
import io.peasoup.inv.io.FileUtils
import io.peasoup.inv.loader.GroovyLoader
import io.peasoup.inv.run.Logger
import org.codehaus.groovy.control.MultipleCompilationErrorsException

@CompileStatic
class SyntaxCommand implements CliCommand {

    List<String> patterns
    String exclude

    int call() {
        assert patterns != null, 'A valid value is required for patterns'
        if (patterns.isEmpty())
            return 1

        // Handle excluding patterns
        def excludePatterns = [".runs/*"]
        if (exclude)
            excludePatterns.add(exclude)

        def commonLoader = new GroovyLoader()
        def syntaxFiles = Pattern.get(patterns, excludePatterns, Home.getCurrent())
        def succeeded = true

        syntaxFiles.each {
            try {
                def result = commonLoader.parseClassFile(it, "empty.package") // TODO Should use its own classloader
                def path = FileUtils.convertUnixPath(it.absolutePath)

                if (result)
                    Logger.info("[SYNTAX] startup succeeded: ${path}")
                else {
                    Logger.warn("[SYNTAX] startup failed: ${path}")
                    succeeded = false
                }
            } catch (MultipleCompilationErrorsException ex) {
                Logger.warn("[SYNTAX] ${ex.getMessage()}")
                succeeded = false
            }
        }

        return succeeded? 0 : 2
    }

    boolean rolling() {
        return false
    }
}
