package io.peasoup.inv.cli

import groovy.io.FileType
import groovy.transform.CompileStatic
import io.peasoup.inv.Home
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

        def invExecutor = new InvExecutor()

        patterns.each {
            def lookupPattern = it

            def lookupFile = new File(lookupPattern)

            if (!lookupFile.isDirectory() && lookupFile.exists())
                invExecutor.read(lookupFile)
            else {

                Logger.debug "[RUN] pattern: ${lookupPattern}"

                // Convert Ant pattern to regex
                def resolvedPattern = lookupPattern
                        .replace("\\", "/")
                        .replace("/", "\\/")
                        .replace(".", "\\.")
                        .replace("*", ".*")
                        .replace("?", ".*")

                Logger.debug "[RUN] resolved_pattern: ${resolvedPattern}"

                List<File> invFiles = []
                Home.getCurrent().eachFileRecurse(FileType.FILES) {

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
                        invExecutor.read(it)

                        progress.step()
                    }
                }
            }
        }

        if (!invExecutor.execute().isOk())
            return -1

        return 0
    }

    boolean rolling() {
        return true
    }
}
