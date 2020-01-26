package io.peasoup.inv.cli

import groovy.io.FileType
import groovy.transform.CompileStatic
import io.peasoup.inv.Main
import io.peasoup.inv.run.InvExecutor
import io.peasoup.inv.run.Logger
import io.peasoup.inv.utils.Progressbar

@CompileStatic
class RunCommand implements CliCommand {

    List<String> patterns
    String exclude

    int call() {
        assert patterns != null, 'A valid value is required for patterns'

        if (patterns.isEmpty())
            return -1

        def executor = new InvExecutor()

        patterns.each {
            def lookupPattern = it

            def lookupFile = new File(lookupPattern)

            if (!lookupFile.isDirectory() && lookupFile.exists())
                executor.read(lookupFile)
            else {

                Logger.debug "pattern without parent: ${lookupPattern}"

                // Convert Ant pattern to regex
                def resolvedPattern = lookupPattern
                        .replace("\\", "/")
                        .replace("/", "\\/")
                        .replace(".", "\\.")
                        .replace("*", ".*")
                        .replace("?", ".*")

                Logger.debug "resolved pattern: ${resolvedPattern}"

                List<File> invFiles = []
                Main.currentHome.eachFileRecurse(FileType.FILES) {

                    // Exclude
                    if (exclude && it.path.contains(exclude))
                        return

                    // Make sure path is using the *nix slash for folders
                    def file = it.path.replace("\\", "/")

                    if (!(file ==~ /.*${resolvedPattern}.*/))
                        return

                    invFiles << it
                }

                def progress = new Progressbar("Reading INV files from args".toString(), invFiles.size(), false)
                progress.start {

                    invFiles.each {
                        executor.read(it)

                        progress.step()
                    }
                }
            }
        }

        executor.execute()

        return 0
    }

    boolean rolling() {
        return true
    }
}