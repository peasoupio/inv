package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.fs.Pattern
import io.peasoup.inv.run.InvExecutor
import io.peasoup.inv.utils.Progressbar

@CompileStatic
class RunCommand implements CliCommand {

    List<String> patterns
    String exclude

    int call() {
        assert patterns != null, 'A valid value is required for patterns'
        if (patterns.isEmpty())
            return -1

        def invExecutor = new InvExecutor()

        def invFiles = Pattern.get(patterns, exclude, Home.getCurrent())
        def progress = new Progressbar("Reading INV files from args".toString(), invFiles.size(), false)
        progress.start {
            invFiles.each {
                invExecutor.read(it)

                progress.step()
            }
        }

        if (!invExecutor.execute().isOk())
            return -1

        return 0
    }

    boolean rolling() {
        return true
    }
}
