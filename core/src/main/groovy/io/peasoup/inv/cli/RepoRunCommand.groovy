package io.peasoup.inv.cli

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.peasoup.inv.repo.RepoExecutor
import io.peasoup.inv.repo.RepoInvoker
import io.peasoup.inv.run.InvExecutor
import io.peasoup.inv.run.Logger
import io.peasoup.inv.run.RunsRoller

import java.nio.file.Files

@CompileStatic
class RepoRunCommand implements CliCommand {

    String repoFileLocation
    Boolean list

    int call() {
        if (!repoFileLocation)
            return -1

        def invExecutor = new InvExecutor()
        def repoExecutor = new RepoExecutor()

        // Check if a single file matches the LIST_FILE_SUFFIX
        if (list) {
            if (!addREPOsFromListFile(repoExecutor))
                return -1
        } else {
            // Otherwise, process patterns normally
            parseRepofile(repoExecutor, repoFileLocation)
        }

        // Execute REPO files
        def reports = repoExecutor.execute()

        // Extracted INVs file
        def invsFiles = reports.collectMany { RepoExecutor.RepoExecutionReport report ->
            return extractINVsFromReports(report)
        } as List<Map>

        // Parse and invoke INV files resolved from REPO
        invsFiles
            .findAll()
            .each {
                invExecutor.parse(
                        it.scriptFile as File,
                        it.path as String,
                        it.name as String)
            }


        Logger.info("[REPO] done")

        if (!invExecutor.execute().isOk())
            return -1

        return 0
    }

    boolean rolling() {
        return true
    }

    private boolean addREPOsFromListFile(RepoExecutor repoExecutor) {
        def repoListPath = repoFileLocation
        def repoListFile = new File(repoListPath)

        if (!repoListFile.exists())
            return false

        Files.copy(repoListFile.toPath(), new File(RunsRoller.latest.folder(), repoListFile.name).toPath())

        Map<String, Map> repoListJson = new JsonSlurper().parse(repoListFile) as Map<String, Map>
        if (!repoListJson || !repoListJson.size())
            return false

        // Parse and incoke REPO file from JSON list file
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

            parseRepofile(repoExecutor, script, expectedParameter)
        }

        return true
    }

    private List extractINVsFromReports(RepoExecutor.RepoExecutionReport report) {
        // If something happened, do not include/try-to-include into the pool
        if (!report.isOk())
            return []

        def name = report.name

        // Manage entry points for REPO
        return report.descriptor.entry.collect {

            def path = report.descriptor.path
            def scriptFile = new File(it)

            if (!scriptFile.exists()) {
                scriptFile = new File(path, it)
                path = scriptFile.parentFile
            }

            if (!scriptFile.exists()) {
                Logger.warn "${scriptFile.canonicalPath} does not exist. Won't run."
                return null
            }

            return [
                    name: name,
                    path: path.canonicalPath,
                    scriptFile: scriptFile
            ]
        }
    }

    private void parseRepofile(RepoExecutor repoExecutor, String repoFileLocation, String expectedParametersFileLocation = null) {
        File localRepofile = new File(repoFileLocation)
        File expectedParametersFile = RepoInvoker.expectedParametersfileLocation(localRepofile)

        if (expectedParametersFileLocation)
            expectedParametersFile = new File(expectedParametersFileLocation);

        repoExecutor.parse(
                localRepofile,
                expectedParametersFile)
    }
}
