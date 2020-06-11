package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.run.Logger
import io.peasoup.inv.scm.ScmExecutor
import org.apache.commons.validator.routines.UrlValidator

@CompileStatic
class InitCommand implements CliCommand {

    String initFileLocation

    int call() {
        assert initFileLocation, 'Scm file path is required'

        ScmExecutor.SCMExecutionReport report = processSCM()
        if (!report) {
            return -1
        }

        // Change currentHome for current process (and others spawned by composer.Execution
        Home.setCurrent(report.repository.path)

        def composerCommand = new ComposerCommand()

        // Define initFile
        composerCommand.settings = [
            initFile: initFileLocation
        ]

        return composerCommand.call()
    }

    boolean rolling() {
        return false
    }

    ScmExecutor.SCMExecutionReport processSCM() {
        String actualFileLocation = initFileLocation
        File scmFile

        // Check if init file location is an URL
        if (UrlValidator.instance.isValid(initFileLocation)) {
            String initFileContent = new URL(initFileLocation).openConnection().inputStream.text

            File tmpFile = new File(System.getenv().TEMP ?: '/tmp', '/inv/init.groovy')
            tmpFile.delete()
            tmpFile << initFileContent

            scmFile = tmpFile
        } else
            scmFile = new File(actualFileLocation)

        assert scmFile.exists(), 'Scm file path must exist on filesystem'

        def scmExecutor = new ScmExecutor()
        scmExecutor.read(scmFile)
        def reports = scmExecutor.execute()

        if (reports.any { !it.isOk() }) {
            //TODO Log a message ?
            return null
        }

        def report = reports.find { it.name.toLowerCase() == "main" }

        if (!report || !report.isOk()) {
            Logger.warn "No named 'main' SCM is defined or available for init."
            return null
        }

        return report
    }
}
