package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.Logger
import io.peasoup.inv.composer.WebServer
import io.peasoup.inv.repo.RepoExecutor
import io.peasoup.inv.repo.RepoURLFetcher
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

        Logger.trace "[COMPOSER] Pulling files from REPO..."
        RepoExecutor.RepoHookExecutionReport report = processREPO(initRepoFileLocation)
        if (!report) {
            return 2
        }

        // Change workspace to the init repo path, but keep current home.
        def initialSettings = [
                initFile : initRepoFileLocation,
                workspace: report.descriptor.getRepoPath().absolutePath
        ]

        Logger.trace "[COMPOSER] Starting server..."
        def composerCommand = new ComposerCommand(initialSettings)
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

        def repoExecutor = new RepoExecutor()

        try {
            // Check if init file location is an URL
            if (UrlValidator.instance.isValid(initRepoFileLocation)) {
                repoExecutor.addScript(initRepoFileLocation)
            } else
                repoExecutor.addScript(new File(actualFileLocation))
        } catch(Exception ex) {
            Logger.warn(ex.getMessage())
            return null
        }

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
