package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.Logger
import io.peasoup.inv.repo.RepoExecutor
import org.apache.commons.validator.routines.UrlValidator
import spark.utils.StringUtils

@CompileStatic
class InitRunCommand implements CliCommand {

    String initRepoFileLocation

    int call() {
        if (StringUtils.isEmpty(initRepoFileLocation))
            return 1

        RepoExecutor.RepoHookExecutionReport report = processREPO()
        if (!report) {
            return 2
        }

        // Change currentHome for current process (and others spawned by composer.Execution
        Home.setCurrent(report.descriptor.getRepoPath())

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

    private RepoExecutor.RepoHookExecutionReport processREPO() {
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
