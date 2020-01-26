package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Main
import io.peasoup.inv.run.Logger
import io.peasoup.inv.scm.ScmExecutor

@CompileStatic
class InitCommand {

    static int call(String scmFilePath) {
        assert scmFilePath, 'Scm file path is required'

        ScmExecutor.SCMReport report = processSCM(new File(scmFilePath))
        if (!report) {
            return -1
        }

        // Change currentHome for current process (and others spawned by composer.Execution
        Main.currentHome = report.repository.path

        ComposerCommand.call()
    }

    static ScmExecutor.SCMReport processSCM(File scmFile) {
        assert scmFile.exists(), 'Scm file path must exist on filesystem'

        def scmExecutor = new ScmExecutor()
        scmExecutor.read(scmFile)
        def reports = scmExecutor.execute()

        if (reports.any { !it.isOk }) {
            //TODO Log a message ?
            return null
        }

        def report = reports.find { it.name.toLowerCase() == "main" }

        if (!report.isOk) {
            Logger.warn "No named 'main' SCM defined for init."
            return null
        }

        return report
    }
}
