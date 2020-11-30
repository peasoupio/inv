package io.peasoup.inv.cli

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import io.peasoup.inv.repo.RepoDescriptor
import io.peasoup.inv.repo.RepoExecutor
import io.peasoup.inv.repo.RepoInvoker
import io.peasoup.inv.repo.RepoURLExtractor

@CompileStatic
class RepoGetCommand implements CliCommand {

    @Override
    int call(Map args = [:]) {
        if (args == null)
            throw new IllegalArgumentException("args")

        String repoUrl = args["<repoUrl>"]
        Boolean createParameters = args["--create-parameters"]
        Boolean run = args["--run"]

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
            return repoRunCommand.call(["<repoFile>": localRepofile.absolutePath])
        }

        return 0
    }

    @Override
    boolean rolling() {
        return true
    }

    @Override
    String usage() {
        """
Get a REPO file from an URL address.

Usage:
  inv [-dsx] repo-get [--run] [--create-parameters] <repoUrl>

Options:
  -r, --run
               Run the REPO file after getting it.
  -c, --create-parameters
               Create a parameter file of a REPO file.

Arguments:
  <repoUrl>    The REPO remote file location.
"""
    }

    private void createNewParametersFile(File localRepofile) {
        RepoExecutor executor = new RepoExecutor()
        executor.addScript(localRepofile)

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
