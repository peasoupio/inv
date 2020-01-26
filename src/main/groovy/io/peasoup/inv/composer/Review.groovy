package io.peasoup.inv.composer

import io.peasoup.inv.Main
import io.peasoup.inv.graph.DeltaGraph
import io.peasoup.inv.run.RunsRoller

class Review {

    private final File baseRun
    private final File latestExecution


    Review(File baseRun, File latestExecution) {
        assert baseRun, 'Base run file is required'
        assert baseRun.exists(), 'Base run file must exist on filesystem'

        assert latestExecution, 'Latest execution file is required'
        assert latestExecution.exists(), 'Latest execution file must exist on filesystem'

        this.baseRun = baseRun
        this.latestExecution = latestExecution


    }

    boolean promote() {
        def envs = System.getenv().collect { "${it.key}=${it.value}".toString() } + ["INV_HOME=${Main.currentHome.absolutePath}".toString()]

        final def myClassPath = System.getProperty("java.class.path")
        final def args = ["java", "-classpath", myClassPath, Main.class.canonicalName, "promote", RunsRoller.latest.folder().name]

        def currentProcess = args.execute(envs, Main.currentHome)
        def exitValue = currentProcess.waitFor()

        return exitValue > -1
    }

    Map toMap() {
        DeltaGraph deltaGraph = new DeltaGraph(baseRun.newReader(), latestExecution.newReader())

        return [
            baseExecution: baseRun.lastModified(),
            lastExecution: latestExecution.lastModified(),
            lines: deltaGraph.deltaLines
        ]
    }

}
