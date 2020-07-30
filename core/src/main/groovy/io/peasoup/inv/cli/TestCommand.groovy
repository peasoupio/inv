package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.fs.Pattern
import io.peasoup.inv.testing.JunitRunner

@CompileStatic
class TestCommand implements CliCommand {

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

        def invFiles = Pattern.get(patterns, excludePatterns, Home.getCurrent())

        def runner = new JunitRunner()

        // Parse INV Groovy files
        invFiles.each {
            runner.add(it.absolutePath)
        }

        return runner.run() ? 0 : 1
    }

    boolean rolling() {
        return true
    }
}
