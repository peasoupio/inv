package io.peasoup.inv.composer

import groovy.transform.CompileStatic
import io.peasoup.inv.graph.DeltaGraph
import io.peasoup.inv.graph.RunGraph
import io.peasoup.inv.run.InvInvoker
import io.peasoup.inv.run.RunsRoller

import java.nio.file.Files

@CompileStatic
class Review {

    private final File baseRun
    private final File latestRun

    private final List<String> removeRepos = []

    Review(File baseRun, File latestRun, RepoFileCollection repos = null) {
        assert baseRun.exists(), 'Base run file must exist on filesystem'
        assert latestRun.exists(), 'Latest execution file must be present on the filesystem'

        this.baseRun = baseRun
        this.latestRun = latestRun

        if (repos != null) {
            def baseRunGraph = new RunGraph(baseRun.newReader())
            def originalRepos = baseRunGraph.files
                    .findAll { it.repo && it.repo != InvInvoker.UNDEFINED_REPO }
                    .collect { it.repo }

            removeRepos.addAll(originalRepos
                    .findAll { !repos.elements.containsKey(it) })
        }
    }

    boolean promote(String appLauncher) {
        def exitValue = MainHelper.execute(
                appLauncher,
                ["promote", RunsRoller.latest.folder().name])

        return exitValue > -1
    }

    /**
     * Allows keeping INVs outside of the subset.
     * Even if a link is missing, it will not be remove.
     * Basically, lines equal, added or missed are kept for the next 'run.txt'
     * Lines flagged as removed are effectively the only ones removed
     *
     * @param baseRun the base run file to compare (can be null or not present on filesystem)
     */
    void merge() {
        def latestBackup = new File(RunsRoller.latest.folder(), "run.backup.txt")
        latestBackup.delete()

        Files.copy(latestRun.toPath(), latestBackup.toPath())
        assert latestBackup.exists(), 'Latest run backup file must be present on filesystem'

        DeltaGraph deltaGraph = new DeltaGraph(baseRun.newReader(), latestRun.newReader())
        deltaGraph.removeRepos(removeRepos)
        deltaGraph.resolve()

        latestRun.delete()
        latestRun << "This file was generated with Composer.${System.lineSeparator()}"
        latestRun.append(deltaGraph.merge())
    }

    Map compare() {
        DeltaGraph deltaGraph = new DeltaGraph(baseRun.newReader(), latestRun.newReader())
        deltaGraph.removeRepos(removeRepos)
        deltaGraph.resolve()

        List<DeltaGraph.DeltaLine> lines = deltaGraph.deltaLines.findAll { it.link.isId() }

        Integer equals = 0, missing = 0, added = 0, removed = 0
        lines.each { DeltaGraph.DeltaLine line ->
            switch (line.state) {
                case '=':
                    equals++
                    break
                case '+':
                    added++
                    break
                case '-':
                    missing++
                    break
                case 'x':
                    removed++
                    break
            }
        }

        return [
                baseExecution: baseRun.lastModified(),
                lastExecution: latestRun.lastModified(),
                lines        : lines,
                stats        : [
                        equals : equals,
                        missing: missing,
                        added  : added,
                        removed: removed

                ]
        ]
    }
}
