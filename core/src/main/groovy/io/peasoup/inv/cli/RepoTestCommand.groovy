package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.loader.FgroupLoader
import io.peasoup.inv.repo.RepoExecutor
import io.peasoup.inv.testing.JunitRunner
import org.apache.commons.lang.RandomStringUtils

import java.nio.file.Path

@CompileStatic
class RepoTestCommand implements CliCommand {

    @Override
    int call(Map args = [:]) {
        if (args == null)
            throw new IllegalArgumentException("args")

        String packageName = "test" + checksum()

        def matches = FgroupLoader.findMatches(Home.getCurrent().absolutePath)

        // Get Repofile name and use it as a package
        if (matches.repoFile != null) {
            def repoExecutor = new RepoExecutor()
            repoExecutor.addScript(matches.repoFile.toFile())

            def repo = repoExecutor.repos.values().first()
            packageName = repo.name
        }

        // Create new runner using the new package
        def runner = new JunitRunner(packageName)

        // Parse groovy classes.
        for(Path groovyFile : matches.groovyFiles) {
            runner.addClass(groovyFile.toString())
        }

        // Add test files
        for(Path groovyFile : matches.groovyTestFiles) {
            runner.addTestScript(groovyFile.toString())
        }

        return runner.run() ? 0 : 2
    }

    @Override
    boolean rolling() {
        return true
    }

    @Override
    String usage() {
        """
Test a REPO folder at the current INV_HOME location.

Usage:
  inv [-dsx] repo-test 
"""
    }

    private String checksum() {
        return RandomStringUtils.random(9, true, true)
    }
}
