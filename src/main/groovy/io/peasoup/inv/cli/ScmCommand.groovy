package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.run.InvExecutor
import io.peasoup.inv.run.Logger
import io.peasoup.inv.run.RunsRoller
import io.peasoup.inv.scm.ScmExecutor
import io.peasoup.inv.utils.Progressbar

import java.nio.file.Files

@CompileStatic
class ScmCommand implements CliCommand {

    List<String> scmFiles

    int call() {
        assert scmFiles != null, 'A valid value is required for args'

        if (scmFiles.isEmpty())
            return -1

        def invExecutor = new InvExecutor()
        def scmExecutor = new ScmExecutor()

        if (scmFiles.size() == 1 && scmFiles[0].endsWith("scm-list.txt")) {
            def scmListPath = scmFiles[0]
            def scmListFile = new File(scmListPath)

            if (!scmListFile.exists())
                return -1

            Files.copy(scmListFile.toPath(), new File(RunsRoller.latest.folder(), scmListFile.name).toPath())

            def lines = scmListFile.readLines()

            def progress = new Progressbar("Reading SCM from '${scmListPath}'".toString(), lines.size(), false)
            progress.start {

                lines.each {
                    scmExecutor.read(new File(it))

                    progress.step()
                }
            }
        } else {
            def progress = new Progressbar("Reading SCM from args".toString(), scmFiles.size(), false)
            progress.start {

                scmFiles.each {
                    scmExecutor.read(new File(it))

                    progress.step()
                }
            }
        }

        def scmFiles = scmExecutor.execute()
        def invsFiles = scmFiles.collectMany { ScmExecutor.SCMReport report ->

            // If something happened, do not include/try-to-include into the pool
            if (!report.isOk)
                return []

            def name = report.name

            // Manage entry points for SCM
            return report.repository.entry.collect {

                def path = report.repository.path
                def scriptFile = new File(it)

                if (!scriptFile.exists()) {
                    scriptFile = new File(path, it)
                    path = scriptFile.parentFile
                }

                if (!scriptFile.exists()) {
                    Logger.warn "${scriptFile.canonicalPath} does not exist. Won't run."
                    return
                }

                return [
                        name: name,
                        path: path.canonicalPath,
                        scriptFile: scriptFile
                ]
            }
        } as List<Map>

        def progress = new Progressbar("Reading INV files from scm".toString(), invsFiles.size(), false)
        progress.start {

            invsFiles.each {
                invExecutor.read(
                        it.path as String,
                        it.scriptFile as File,
                        it.name as String)

                progress.step()
            }
        }

        Logger.info("[SCM] done")

        invExecutor.execute()

        return 0
    }

    boolean rolling() {
        return true
    }
}