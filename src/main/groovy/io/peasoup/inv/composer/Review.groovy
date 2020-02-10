package io.peasoup.inv.composer

import io.peasoup.inv.Main
import io.peasoup.inv.graph.DeltaGraph
import io.peasoup.inv.run.RunsRoller

class Review {

    boolean promote() {
        def envs = System.getenv().collect { "${it.key}=${it.value}".toString() } + ["INV_HOME=${Main.currentHome.absolutePath}".toString()]

        final def myClassPath = System.getProperty("java.class.path")
        final def args = ["java", "-classpath", myClassPath, Main.class.canonicalName, "promote", RunsRoller.latest.folder().name]

        def currentProcess = args.execute(envs, Main.currentHome)
        def exitValue = currentProcess.waitFor()

        return exitValue > -1
    }

    Map compare(File baseRun, File latestExecution) {
        assert baseRun, 'Base run file is required'
        assert baseRun.exists(), 'Base run file must exist on filesystem'

        assert latestExecution, 'Latest execution file is required'

        DeltaGraph deltaGraph = new DeltaGraph(baseRun.newReader(), latestExecution.newReader())

        Integer equals = 0, missing = 0, added = 0, removed = 0
        deltaGraph.deltaLines.each { DeltaGraph.DeltaLine line ->
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
            lines: deltaGraph.deltaLines,
            stats: [
                equals: equals,
                missing: missing,
                added: added,
                removed: removed

            ]
        ]
    }

}
