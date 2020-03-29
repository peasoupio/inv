package io.peasoup.inv.composer

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.Main
import io.peasoup.inv.graph.DeltaGraph
import io.peasoup.inv.run.RunsRoller

import java.nio.file.Files

@CompileStatic
class Review {

    @CompileDynamic
    boolean promote() {
        def envs = System.getenv().collect { "${it.key}=${it.value}".toString() } + ["INV_HOME=${Home.getCurrent().absolutePath}".toString()]

        final def myClassPath = System.getProperty("java.class.path")
        final def args = ["java", "-classpath", myClassPath, Main.class.canonicalName, "promote", RunsRoller.latest.folder().name]

        def currentProcess = args.execute(envs, Home.getCurrent())
        currentProcess.waitForProcessOutput()

        def exitValue = currentProcess.exitValue()

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
    @CompileDynamic
    void mergeWithBase(File baseRun) {
        if (!baseRun)
            return

        if (!baseRun.exists())
            baseRun.exists()

        def latestRun = new File(RunsRoller.latest.folder(), "run.txt")
        def latestBackup = new File(RunsRoller.latest.folder(), "run.backup.txt")
        if (latestRun.exists()) {
            latestBackup.delete()
            Files.copy(latestRun.toPath(), latestBackup.toPath())
            latestRun.delete()
        }

        assert latestBackup.exists(), 'Latest run backup file must be present on filesystem'

        def generatedRun = new File(RunsRoller.latest.folder(), "run.txt")
        generatedRun.delete()

        DeltaGraph deltaGraph = new DeltaGraph(baseRun.newReader(), latestBackup.newReader())

        generatedRun << "This file was generated with Composer.${System.lineSeparator()}"
        generatedRun.append(deltaGraph.merge())
    }

    @CompileDynamic
    Map compare(File baseRun, File latestExecution) {
        assert baseRun, 'Base run file is required'
        assert baseRun.exists(), 'Base run file must exist on filesystem'

        assert latestExecution, 'Latest execution file is required'
        assert latestExecution.exists(), 'Latest execution file must be present on the filesystem'

        DeltaGraph deltaGraph = new DeltaGraph(baseRun.newReader(), latestExecution.newReader())
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
            lastExecution: latestExecution.lastModified(),
            lines: lines,
            stats: [
                equals: equals,
                missing: missing,
                added: added,
                removed: removed

            ]
        ]
    }



}
