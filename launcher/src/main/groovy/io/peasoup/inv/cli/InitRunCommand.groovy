package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.Logger
import io.peasoup.inv.composer.WebServer
import io.peasoup.inv.repo.RepoExecutor
import org.apache.commons.validator.routines.UrlValidator
import spark.utils.StringUtils

@CompileStatic
class InitRunCommand implements CliCommand {

    @Override
    int call(Map args = [:]) {
        if (args == null)
            throw new IllegalArgumentException("args")

        String initRepoFileLocation = args["<repoFile>"]

        if (StringUtils.isEmpty(initRepoFileLocation))
            return 1

        RepoExecutor.RepoHookExecutionReport report = processREPO(initRepoFileLocation)
        if (!report) {
            return 2
        }

        // Change currentHome for current process (and others spawned by composer.Execution
        Home.setCurrent(report.descriptor.getRepoPath())

        def composerCommand = new ComposerCommand(initRepoFileLocation)
        return composerCommand.call(args)
    }

    @Override
    boolean rolling() {
        return false
    }

    @Override
    String usage() {
        """
Start Composer using an INIT file.

Usage:
  inv [-dsx] init-run [-p <port>] <repoFile>

Options:
  -p, --port=port
               Sets the listening port 

Arguments:
  <repoFile>   The REPO file location.
    
Environment variables:
  ${WebServer.CONFIG_SSL_KEYSTORE}  Sets the SSL keystore location
  ${WebServer.CONFIG_SSL_PASSWORD}  Sets the SSL keystore password
"""
    }

    private RepoExecutor.RepoHookExecutionReport processREPO(String initRepoFileLocation) {
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

        if (!repoFile.exists())
            return null

        def repoExecutor = new RepoExecutor()
        repoExecutor.addScript(repoFile)
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
