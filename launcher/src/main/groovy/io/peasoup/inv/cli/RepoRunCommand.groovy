package io.peasoup.inv.cli

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.Logger
import io.peasoup.inv.repo.RepoFolderCollection
import io.peasoup.inv.run.InvExecutor
import io.peasoup.inv.run.RunsRoller

import java.nio.file.Files

@CompileStatic
class RepoRunCommand implements CliCommand {

    @Override
    int call(Map args = [:]) {
        if (args == null)
            throw new IllegalArgumentException("args")

        String repoFileLocation = args["<repoFile>"]
        Boolean list = args["--list"]

        if (!repoFileLocation)
            return 1

        def invExecutor = new InvExecutor()
        def repoFolders = new RepoFolderCollection(invExecutor)

        // Check if a single file matches the LIST_FILE_SUFFIX
        if (list) {
            boolean listResult = readListJsonfile(repoFileLocation, repoFolders)
            if (!listResult)
                return 2
        } else {
            boolean singleResult = readSinglefile(repoFileLocation, repoFolders)
            if (!singleResult)
                return 3
        }

        // Read repos and load invs
        if (!repoFolders.loadInvs())
            return 4

        // Execute invs
        if (!invExecutor.execute().isOk())
            return 5

        return 0
    }

    @Override
    boolean rolling() {
        return true
    }

    @Override
    String usage() {
        """
Execute a REPO file from.

Usage:
  inv [-dsx] repo-run [--list] <repoFile>

Options:
  -l, --list 
               Use a list of repo to run.

Arguments:
  <repoFile>   The REPO file location. 
"""
    }

    private boolean readListJsonfile(String repoFileLocation, RepoFolderCollection repoFolders) {
        def repoListPath = repoFileLocation
        def repoListFile = new File(repoListPath)

        if (!repoListFile.exists())
            return false

        Files.copy(repoListFile.toPath(), new File(RunsRoller.latest.folder(), repoListFile.name).toPath())

        Map<String, Map> repoListJson = new JsonSlurper().parse(repoListFile) as Map<String, Map>
        if (!repoListJson || !repoListJson.size())
            return false

        // Parse and invoke REPO file from JSON list file
        repoListJson.each { String name, Map repo ->
            String script = "", expectedParameter = ""

            if (repo.containsKey("script"))
                script = repo.get("script") as String

            if (repo.containsKey("expectedParameterFile"))
                expectedParameter = repo.get("expectedParameterFile") as String

            if (!script) {
                Logger.warn("[REPO] name: ${name} as no script defined")
                return
            }

            // If script path is relative, try to resolve from the current home path
            if (!new File(script).isAbsolute())
                script = new File(Home.getCurrent(), script).absolutePath

            // If expected parameters path is relative, try to resolve from the current home path
            if (expectedParameter && !new File(expectedParameter).isAbsolute())
                expectedParameter = new File(Home.getCurrent(), expectedParameter).absolutePath

            repoFolders.add(script, expectedParameter)
        }

        return true
    }

    private boolean readSinglefile(String repoFileLocation, RepoFolderCollection repoFolders) {
        return repoFolders.add(new File(repoFileLocation).absolutePath)
    }
}
