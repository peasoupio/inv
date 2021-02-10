package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.Logger
import io.peasoup.inv.fs.Pattern
import io.peasoup.inv.io.FileUtils
import io.peasoup.inv.loader.GroovyLoader
import org.codehaus.groovy.control.MultipleCompilationErrorsException

@CompileStatic
class SyntaxCommand implements CliCommand {

    @Override
    int call(Map args = [:]) {
        if (args == null)
            throw new IllegalArgumentException("args")

        List<String> includes = args["<include>"] as List<String>
        String exclude = args["--exclude"]

        assert includes != null, 'A valid value is required for patterns'
        if (includes.isEmpty())
            return 1

        // Handle excluding patterns
        def excludePatterns = [".runs/*"]
        if (exclude)
            excludePatterns.add(exclude)

        def commonLoader = GroovyLoader.newBuilder().build()
        def syntaxFiles = Pattern.get(includes, excludePatterns, Home.getCurrent())
        def succeeded = true

        syntaxFiles.each {
            try {
                def result = commonLoader.parseClassFile(it, "empty.package")
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

    @Override
    boolean rolling() {
        return true
    }

    @Override
    String usage() {
        """
Check the syntax of an INV or REPO file.

Usage:
  inv [-dsx] syntax [--exclude <exclude>] <include>...

Options:
  -e, --exclude=exclude
               Indicates the files to exclude.
               Exclusion is predominant over inclusion
               It is Ant-compatible 
               (p.e *.groovy, ./**/*.groovy, ...)

Arguments:
  <include>    Indicates the files to include.
               It is Ant-compatible 
               (p.e *.groovy, ./**/*.groovy, ...)
               It is also expandable using a space-separator
               (p.e myfile1.groovy myfile2.groovy)
"""
    }
}
