package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.run.Logger
import io.peasoup.inv.scm.ScmExecutor

@CompileStatic
class InitCommand implements CliCommand {

    String scmFilePath

    int call() {
        assert scmFilePath, 'Scm file path is required'

        ScmExecutor.SCMReport report = processSCM(new File(scmFilePath))
        if (!report) {
            return -1
        }

        // Change currentHome for current process (and others spawned by composer.Execution
        Home.setCurrent(report.repository.path)

        def composerCommand = new ComposerCommand()

        // Define initFile
        composerCommand.settings = [
            initFile: scmFilePath
        ]

        return composerCommand.call()
    }

    boolean rolling() {
        return false
    }

    protected ScmExecutor.SCMReport processSCM(File scmFile) {
        assert scmFile.exists(), 'Scm file path must exist on filesystem'

        def scmExecutor = new ScmExecutor()
        scmExecutor.read(scmFile)
        def reports = scmExecutor.execute()

        if (reports.any { !it.isOk }) {
            //TODO Log a message ?
            return null
        }

        def report = reports.find { it.name.toLowerCase() == "main" }

        if (!report || !report.isOk) {
            Logger.warn "No named 'main' SCM is defined or available for init."
            return null
        }

        return report
    }
}
