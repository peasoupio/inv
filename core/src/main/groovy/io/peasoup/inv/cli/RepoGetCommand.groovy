package io.peasoup.inv.cli

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import io.peasoup.inv.repo.RepoDescriptor
import io.peasoup.inv.repo.RepoExecutor
import io.peasoup.inv.repo.RepoInvoker
import io.peasoup.inv.repo.RepoURLExtractor

@CompileStatic
class RepoGetCommand implements CliCommand {

    String repoUrl
    Boolean createParameters
    Boolean run

    int call() {
        if (!repoUrl)
            return 1

        def localRepofile = RepoURLExtractor.extract(repoUrl)
        if (localRepofile == null)
            return 2

        if (createParameters) {
            createNewParametersFile(localRepofile)
        }

        if (run) {
            RepoRunCommand repoRunCommand = new RepoRunCommand()
            repoRunCommand.repoFileLocation = localRepofile.absolutePath
            return repoRunCommand.call()
        }

        return 0
    }

    boolean rolling() {
        return true
    }

    private void createNewParametersFile(File localRepofile) {
        RepoExecutor executor = new RepoExecutor()
        RepoInvoker.invoke(executor, localRepofile)

        // Get expected parameter file location
        File newParameterFile = RepoInvoker.expectedParametersfileLocation(localRepofile)
        if (newParameterFile.exists())
            newParameterFile.delete()

        Map<String, Map> jsonOutput = [:]
        for(RepoDescriptor repoDescriptor: executor.getRepos().values()) {

            // Get parameters and theirs values
            Map<String, Object> parametersValues = [:]
            for (RepoDescriptor.AskParameter parameter : repoDescriptor.getAsk()?.getParameters()) {
                parametersValues.put(parameter.getName(), parameter.getDefaultValue() ?: null)
            }

            jsonOutput << ([(repoDescriptor.getName()): parametersValues] as Map<String, Map>)
        }

        // Save JSON into parameters file
        newParameterFile << JsonOutput.prettyPrint(JsonOutput.toJson(jsonOutput))
    }
}
