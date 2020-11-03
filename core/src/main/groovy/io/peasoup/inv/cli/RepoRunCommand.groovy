package io.peasoup.inv.cli

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.peasoup.inv.loader.FgroupLoader
import io.peasoup.inv.repo.RepoDescriptor
import io.peasoup.inv.repo.RepoExecutor
import io.peasoup.inv.repo.RepoInvoker
import io.peasoup.inv.run.InvExecutor
import io.peasoup.inv.run.InvInvoker
import io.peasoup.inv.run.Logger
import io.peasoup.inv.run.RunsRoller

import java.nio.file.Files
import java.nio.file.Path

@CompileStatic
class RepoRunCommand implements CliCommand {

    String repoFileLocation
    Boolean list

    int call() {
        if (!repoFileLocation)
            return 1

        def invExecutor = new InvExecutor()

        // Check if a single file matches the LIST_FILE_SUFFIX
        if (list) {
            boolean listResult = readListJsonfile(invExecutor)
            if (!listResult)
                return 2
        } else {
            boolean singleResult = readSinglefile(invExecutor)
            if (!singleResult)
                return 3
        }

        if (!invExecutor.execute().isOk())
            return 4

        return 0
    }

    boolean rolling() {
        return true
    }

    private boolean readListJsonfile(InvExecutor invExecutor) {
        def repoListPath = repoFileLocation
        def repoListFile = new File(repoListPath)

        if (!repoListFile.exists())
            return false

        Files.copy(repoListFile.toPath(), new File(RunsRoller.latest.folder(), repoListFile.name).toPath())

        Map<String, Map> repoListJson = new JsonSlurper().parse(repoListFile) as Map<String, Map>
        if (!repoListJson || !repoListJson.size())
            return false

        RepoExecutor repoExecutor = new RepoExecutor()

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

        // Extracted INVs file
        for(RepoExecutor.RepoExecutionReport report : repoExecutor.execute()) {
            parseDescriptorfiles(invExecutor, report.descriptor)
        }

        return true
    }

    private boolean readSinglefile(InvExecutor invExecutor) {
        def repoExecutor = new RepoExecutor()

        // If a single file, assume its an repo file
        if (new File(repoFileLocation).isFile()) {

            parseRepofile(repoExecutor, repoFileLocation)

            // Execute REPO files
            def reports = repoExecutor.execute()

            // Extracted INVs file
            for (RepoExecutor.RepoExecutionReport report : reports) {
                if (!report.isOk())
                    continue

                parseDescriptorfiles(invExecutor, report.descriptor)
            }
        } else {
            // Otherwise, expect a repo folder
            def matches = FgroupLoader.findMatches(repoFileLocation)

            if (matches.scmFile == null)
                return false

            parseRepofile(repoExecutor, matches.scmFile.toString())
            RepoDescriptor repoDescriptor = repoExecutor.getRepos().values().first()

            parseDescriptorfiles(invExecutor, repoDescriptor, matches)
        }

        return true
    }

    private void parseRepofile(RepoExecutor repoExecutor, String repoFileLocation, String expectedParametersFileLocation = null) {
        File localRepofile = new File(repoFileLocation)
        File expectedParametersFile = RepoInvoker.expectedParametersfileLocation(localRepofile)

        if (expectedParametersFileLocation)
            expectedParametersFile = new File(expectedParametersFileLocation)

        repoExecutor.parse(
                localRepofile,
                expectedParametersFile)
    }

    private void parseDescriptorfiles(InvExecutor invExecutor, RepoDescriptor descriptor) {
        parseDescriptorfiles(
                invExecutor,
                descriptor,
                FgroupLoader.findMatches(descriptor.getRepoCompletePath().absolutePath))
    }

    private void parseDescriptorfiles(InvExecutor invExecutor, RepoDescriptor descriptor, FgroupLoader.InvMatches matches) {
        def path = matches.rootPath
        def name = descriptor.name
        def newPackage = name

        // Invoke groovy files
        invokegroovyfiles(matches, newPackage)

        // Invoke inv files
        invokeInvfiles(matches, invExecutor, newPackage, path, name)
    }

    private void invokegroovyfiles(FgroupLoader.InvMatches matches, String newPackage) {
        // Parse inv files.
        for(Path groovyFile : matches.groovyFiles) {
            InvInvoker.addClass(groovyFile.toFile(), newPackage)
        }
    }

    private void invokeInvfiles(FgroupLoader.InvMatches matches, InvExecutor invExecutor, String newPackage, String path, String repo) {
        // Parse inv files.
        for(Path invFile : matches.invFiles) {
            invExecutor.parse(
                    invFile.toFile(),
                    newPackage,
                    path,
                    repo)
        }
    }




}
