package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.fs.Pattern
import io.peasoup.inv.run.InvExecutor

@CompileStatic
class RunCommand implements CliCommand {

    List<String> patterns
    String exclude

    int call() {
        if (patterns == null)
            return 1

        if (patterns.isEmpty())
            return 2

        // Handle excluding patterns
        def excludePatterns = [".runs/*"]
        if (exclude)
            excludePatterns.add(exclude)

        def invExecutor = new InvExecutor()

        def invFiles = Pattern.get(patterns, excludePatterns, Home.getCurrent())

        // Parse INV Groovy files
        invFiles.each {
            invExecutor.parse(it)
        }

        // Do the actual execution
        if (!invExecutor.execute().isOk())
            return 3

        return 0
    }

    boolean rolling() {
        return true
    }
}
