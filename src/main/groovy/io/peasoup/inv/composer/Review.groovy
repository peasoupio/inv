package io.peasoup.inv.composer


import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.Main
import io.peasoup.inv.graph.DeltaGraph
import io.peasoup.inv.graph.RunGraph
import io.peasoup.inv.run.InvInvoker
import io.peasoup.inv.run.RunsRoller

import java.nio.file.Files

@CompileStatic
class Review {

    private final File baseRun
    private final File latestRun

    private final List<String> removeScms = []

    Review(File baseRun, File latestRun, ScmFileCollection scms = null) {
        assert baseRun.exists(), 'Base run file must exist on filesystem'
        assert latestRun.exists(), 'Latest execution file must be present on the filesystem'

        this.baseRun = baseRun
        this.latestRun = latestRun

        if (scms != null) {
            def baseRunGraph = new RunGraph(baseRun.newReader())
            def originalScms = baseRunGraph.files
                    .findAll { it.scm && it.scm != InvInvoker.UNDEFINED_SCM }
                    .collect { it.scm }

            removeScms.addAll(originalScms
                    .findAll { !scms.elements.containsKey(it) })
        }
    }

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
    void merge() {
        def latestBackup = new File(RunsRoller.latest.folder(), "run.backup.txt")
        latestBackup.delete()

        Files.copy(latestRun.toPath(), latestBackup.toPath())
        assert latestBackup.exists(), 'Latest run backup file must be present on filesystem'

        def generatedRun = new File(RunsRoller.latest.folder(), "run.txt")
        generatedRun.delete()

        DeltaGraph deltaGraph = new DeltaGraph(baseRun.newReader(), latestBackup.newReader())
        deltaGraph.removeScms(removeScms)
        deltaGraph.resolve()

        generatedRun << "This file was generated with Composer.${System.lineSeparator()}"
        generatedRun.append(deltaGraph.merge())
    }

    Map compare() {
        DeltaGraph deltaGraph = new DeltaGraph(baseRun.newReader(), latestRun.newReader())
        deltaGraph.removeScms(removeScms)
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
