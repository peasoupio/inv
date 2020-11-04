package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.loader.FgroupLoader
import io.peasoup.inv.repo.RepoExecutor
import io.peasoup.inv.run.InvInvoker
import io.peasoup.inv.testing.JunitRunner
import org.apache.commons.lang.RandomStringUtils

import java.nio.file.Path

@CompileStatic
class RepoTestCommand implements CliCommand {

    int call() {
        String newPackage = "test" + checksum()

        def matches = FgroupLoader.findMatches(Home.getCurrent().absolutePath)

        // Get Repofile name and use it as a package
        if (matches.repoFile != null) {
            def repoExecutor = new RepoExecutor()
            repoExecutor.parse(matches.repoFile.toFile())

            def repo = repoExecutor.repos.values().first()
            newPackage = repo.name
        }

        // Parse groovy classes.
        for(Path groovyFile : matches.groovyFiles) {
            InvInvoker.addClass(groovyFile.toFile(), newPackage)
        }

        // Create new runner using the new package
        def runner = new JunitRunner(newPackage)

        // Add test files
        for(Path groovyFile : matches.groovyTestFiles) {
            runner.add(groovyFile.toString())
        }

        return runner.run() ? 0 : 2
    }

    boolean rolling() {
        return true
    }

    private String checksum() {
        return RandomStringUtils.random(9, true, true)
    }
}
