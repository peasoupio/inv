package io.peasoup.inv

import groovy.json.JsonSlurper
import groovy.text.SimpleTemplateEngine

class ScmReader {

    private final Map scm

    ScmReader(File scmFile) {

        // Parse JSON file into object
        scm = new JsonSlurper().parse(scmFile)

        // For each SCM, we parse hooks using a template engine
        def templateEngine = new SimpleTemplateEngine()

        scm.each { String name, Map repository ->

            if (!repository.hooks)
                return

            def binding = [
                src:  repository.src,
                name: name,
                env:  System.getenv()
            ]

            repository.path = binding.path =
                    new File(
                            templateEngine.createTemplate(repository.path).make(binding).toString()
                    ).canonicalPath

            repository.hooks = repository.hooks.collectEntries { String hookName, List<String> commands ->
                [
                    (hookName):
                    commands.collect { String command ->
                        templateEngine.createTemplate(command).make(binding)
                            .toString()
                            .replace("\\", "/") // normalize path
                    }
                ]
            }

        }
    }

    Map<String, File> execute() {

        return scm.collectEntries { String name, Map repository ->

            if (!repository.hooks)
                return

            File path = new File(repository.path)

            if (!path.exists()) {

                Logger.info("[SCM] ${name} [INIT] start")
                executeCommands path, repository.hooks["init"]
                Logger.info("[SCM] ${name} [INIT] done")
            } else {

                Logger.info("[SCM] ${name} [UPDATE] start")
                executeCommands path, repository.hooks["update"]
                Logger.info("[SCM] ${name} [UPDATE] done")
            }

            return [(name): new File(path, repository.entry)]
        }
    }

    private void executeCommands(File path, List<String> commands) {

        def shFile = new File(path.parent, ".scm-sh")

        shFile.mkdirs()

        if (shFile.exists())
            shFile.delete()

        commands.each {
            shFile << it
            shFile << System.lineSeparator()
        }

        def process = "sh ${shFile.canonicalPath}".execute()
        process.consumeProcessOutput(System.out, System.err)
        process.waitForOrKill(60000) //TODO Needs to bo configurable from the SCM file
    }
}