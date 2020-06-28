package io.peasoup.inv.cli


import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.fs.Pattern
import io.peasoup.inv.run.InvExecutor
import io.peasoup.inv.run.Logger
import io.peasoup.inv.run.RunsRoller
import io.peasoup.inv.scm.ScmExecutor
import io.peasoup.inv.scm.ScmInvoker

import java.nio.file.Files

@CompileStatic
class ScmCommand implements CliCommand {

    public static final String LIST_FILE_SUFFIX = 'scm-list.json'

    List<String> patterns
    String exclude

    int call() {
        assert patterns != null, 'A valid value is required for patterns'
        if (patterns.isEmpty())
            return -1

        def invExecutor = new InvExecutor()
        def scmExecutor = new ScmExecutor()

        if (patterns.size() == 1 && patterns[0].endsWith(LIST_FILE_SUFFIX)) {
            def scmListPath = patterns[0]
            def scmListFile = new File(scmListPath)

            if (!scmListFile.exists())
                return -1

            Files.copy(scmListFile.toPath(), new File(RunsRoller.latest.folder(), scmListFile.name).toPath())

            Map<String, Map> scmListJson = new JsonSlurper().parse(scmListFile) as Map<String, Map>
            if (!scmListJson || !scmListJson.size())
                return -2

            // Parse and incoke SCM file from JSON list file
            scmListJson.each { String name, Map scm ->
                String script = "", expectedParameter = ""

                if (scm.containsKey("script"))
                    script = scm.get("script") as String

                if (scm.containsKey("expectedParameterFile"))
                    expectedParameter = scm.get("expectedParameterFile") as String

                if (!script) {
                    Logger.warn("[SCM] name: ${name} as no script defined")
                    return
                }

                if (!expectedParameter)
                    scmExecutor.parse(new File(script))
                else
                    scmExecutor.parse(new File(script), new File(expectedParameter))
            }
        } else {
            // Handle excluding patterns
            def excludePatterns = ScmInvoker.DEFAULT_EXCLUDED.toList()
            if (exclude)
                excludePatterns.add(exclude)

            def scmFiles = Pattern.get(patterns, excludePatterns, Home.getCurrent())

            // Parse and incoke SCM file
            scmFiles.each {
                scmExecutor.parse(it, ScmInvoker.expectedParametersfileLocation(it))
            }
        }

        def scmFiles = scmExecutor.execute()
        def invsFiles = scmFiles.collectMany { ScmExecutor.SCMExecutionReport report ->

            // If something happened, do not include/try-to-include into the pool
            if (!report.isOk())
                return []

            def name = report.name

            // Manage entry points for SCM
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
        } as List<Map>

        // Parse and invoke INV files resolved from SCM
        invsFiles
            .findAll()
            .each {
                invExecutor.parse(
                        it.scriptFile as File,
                        it.path as String,
                        it.name as String)
            }


        Logger.info("[SCM] done")

        if (!invExecutor.execute().isOk())
            return -1

        return 0
    }

    boolean rolling() {
        return true
    }
}
