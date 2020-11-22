package io.peasoup.inv.cli

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.peasoup.inv.Logger
import io.peasoup.inv.repo.RepoFolderCollection
import io.peasoup.inv.run.InvExecutor
import io.peasoup.inv.run.RunsRoller

import java.nio.file.Files

@CompileStatic
class RepoRunCommand implements CliCommand {

    String repoFileLocation
    Boolean list

    int call() {
        if (!repoFileLocation)
            return 1

        def invExecutor = new InvExecutor()
        def repoFolders = new RepoFolderCollection(invExecutor)

        // Check if a single file matches the LIST_FILE_SUFFIX
        if (list) {
            boolean listResult = readListJsonfile(repoFolders)
            if (!listResult)
                return 2
        } else {
            boolean singleResult = readSinglefile(repoFolders)
            if (!singleResult)
                return 3
        }

        // Read repos and load invs
        repoFolders.loadInvs()

        // Execute invs
        if (!invExecutor.execute().isOk())
            return 4

        return 0
    }

    boolean rolling() {
        return true
    }

    private boolean readListJsonfile(RepoFolderCollection repoFolders) {
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

            repoFolders.add(script, expectedParameter)
        }

        return true
    }

    private boolean readSinglefile(RepoFolderCollection repoFolders) {
        return repoFolders.add(new File(repoFileLocation).absolutePath)
    }
}
