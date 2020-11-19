package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.Logger
import io.peasoup.inv.io.FileUtils

@CompileStatic
class RepoCreateCommand implements CliCommand {

    int call() {
        def invDir = new File(Home.getCurrent(), ".inv/")
        invDir.mkdirs()

        def srcFiles = new File(invDir, "src/")
        srcFiles.mkdirs()

        def testFiles = new File(invDir, "test/")
        testFiles.mkdirs()

        def varsFiles = new File(invDir, "vars/")
        varsFiles.mkdirs()

        def repoFile = new File(invDir,"repo.yml")
        if (!repoFile.exists())
            repoFile << "# Newly created repo file"

        Logger.system("SRCFILES: ${FileUtils.convertUnixPath(srcFiles.absolutePath)}")
        Logger.system("TESTFILES: ${FileUtils.convertUnixPath(testFiles.absolutePath)}")
        Logger.system("VARSFILES: ${FileUtils.convertUnixPath(varsFiles.absolutePath)}")
        Logger.system("REPOFILE: ${FileUtils.convertUnixPath(repoFile.absolutePath)}")

        return 0
    }

    boolean rolling() {
        return false
    }


}
