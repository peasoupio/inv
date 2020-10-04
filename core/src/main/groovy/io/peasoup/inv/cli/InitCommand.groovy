package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.repo.RepoExecutor
import io.peasoup.inv.run.Logger
import org.apache.commons.validator.routines.UrlValidator

@CompileStatic
class InitCommand implements CliCommand {

    String initRepoFileLocation

    int call() {
        assert initRepoFileLocation, 'REPO file path is required'

        RepoExecutor.RepoExecutionReport report = processREPO()
        if (!report) {
            return -1
        }

        // Change currentHome for current process (and others spawned by composer.Execution
        Home.setCurrent(report.descriptor.path)

        def composerCommand = new ComposerCommand()

        // Define initFile
        composerCommand.settings = [
            initFile: initRepoFileLocation
        ]

        return composerCommand.call()
    }

    boolean rolling() {
        return false
    }

    RepoExecutor.RepoExecutionReport processREPO() {
        String actualFileLocation = initRepoFileLocation
        File repoFile

        // Check if init file location is an URL
        if (UrlValidator.instance.isValid(initRepoFileLocation)) {
            String initFileContent = new URL(initRepoFileLocation).openConnection().inputStream.text

            File tmpFile = new File(System.getenv().TEMP ?: '/tmp', '/inv/init.groovy')
            tmpFile.delete()
            tmpFile << initFileContent

            repoFile = tmpFile
        } else
            repoFile = new File(actualFileLocation)

        assert repoFile.exists(), 'Repo file path must exist on filesystem'

        def repoExecutor = new RepoExecutor()
        repoExecutor.parse(repoFile)
        def reports = repoExecutor.execute()

        if (reports.any { !it.isOk() }) {
            //TODO Log a message ?
            return null
        }

        def report = reports.find { it.name.toLowerCase() == "main" }

        if (!report || !report.isOk()) {
            Logger.warn "No named 'main' REPO is defined or available for init."
            return null
        }

        return report
    }
}
