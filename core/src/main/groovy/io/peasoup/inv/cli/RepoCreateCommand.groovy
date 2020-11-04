package io.peasoup.inv.cli

import groovy.transform.CompileStatic

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

        def repoFile = new File(invDir,"repo.yml")
        if (!repoFile.exists())
            repoFile << "# Newly created repo file"

        return 0
    }

    boolean rolling() {
        return false
    }


}
