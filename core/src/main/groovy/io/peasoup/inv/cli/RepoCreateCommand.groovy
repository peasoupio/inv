package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.repo.RepoExecutor
import io.peasoup.inv.run.Logger
import org.apache.commons.validator.routines.UrlValidator

@CompileStatic
class RepoCreateCommand implements CliCommand {

    int call() {

        def invDir = new File(".inv/")
        invDir.mkdirs()

        def srcFiles = new File(invDir, "src/")
        srcFiles.mkdirs()

        def testFiles = new File(invDir, "test/")
        testFiles.mkdirs()

        def varsFiles = new File(invDir, "vars/")
        varsFiles.mkdirs()

        def scmFile = new File(invDir,"scm.yml")
        scmFile << "# Newly created scm file"

        return 0
    }

    boolean rolling() {
        return false
    }


}
