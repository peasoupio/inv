package io.peasoup.inv

import groovy.json.JsonSlurper
import groovy.text.SimpleTemplateEngine

class ScmReader {

    final Map<String, Repository> scm

    ScmReader(File scmFile) {
        this(scmFile.newReader())
    }

    ScmReader(Reader reader) {

        // Parse JSON file into object
        Map json = new JsonSlurper().parse(reader)

        // For each SCM, we parse hooks using a template engine
        def templateEngine = new SimpleTemplateEngine()

        scm = json.collectEntries { String name, Map repository ->

            if (!repository.hooks)
                return

            def binding = [
                src:  repository.src,
                name: name,
                env:  System.getenv()
            ]

            binding.path = new File(templateEngine.createTemplate(repository.path).make(binding).toString())

            return [(name): new Repository(
                name: binding.name,
                path: binding.path,
                src: repository.src,
                entry: repository.entry ?: "",
                timeout: repository.timeout,
                hooks: repository.hooks.collectEntries { String hookName, List<String> commands -> [(hookName):
                    commands.collect { String command ->
                        templateEngine.createTemplate(command).make(binding)
                            .toString()
                            .replace("\\", "/") // normalize path
                    }
                ]}
            )]
        }
    }

    Map<String, File> execute() {

        return scm.collectEntries { String name, Repository repository ->

            if (!repository.hooks)
                return

            if (!repository.path.exists()) {

                Logger.info("[SCM] ${name} [INIT] start")
                executeCommands repository, repository.hooks["init"]
                Logger.info("[SCM] ${name} [INIT] done")
            } else {

                Logger.info("[SCM] ${name} [UPDATE] start")
                executeCommands repository, repository.hooks["update"]
                Logger.info("[SCM] ${name} [UPDATE] done")
            }

            return [(name): new File(repository.path, repository.entry)]
        }
    }

    private void executeCommands(Repository repository, List<String> commands) {

        // Create file and dirs for the SH file
        def shFile = new File(repository.path.parent, ".scm-sh")
        shFile.mkdirs()

        // Make sure we're using the latest version of the commands
        if (shFile.exists())
            shFile.delete()

        // Stack them into the SH file
        commands.each {
            shFile << it
            shFile << System.lineSeparator()
        }

        // Calling the SH file with the commands in it
        // We can't let the runtime decide of the executing folder, so we're using the parent folder of the SH File
        def cmd = "sh ${shFile.canonicalPath}"
        def process = cmd.execute(null, new File(repository.path.parent))

        Logger.debug cmd

        // Consome output and wait until done.
        process.consumeProcessOutput(System.out, System.err)
        process.waitForOrKill(repository.timeout ?: 60000)
    }

    private static class Repository {

        String name
        File path
        String src
        String entry
        Integer timeout

        Map<String, List<String>> hooks
    }
}