package io.peasoup.inv.composer

import io.peasoup.inv.Home

class MainHelper {

    private MainHelper() {
        // private ctor
    }

    static List<String> createArgs(String launcher, List<String> args) {
        return ["java", "-jar", launcher] + args
    }

    static int execute(String launcher, List<String> args) {
        def envs = System.getenv().collect { "${it.key}=${it.value}".toString() } + ["INV_HOME=${Home.getCurrent().absolutePath}".toString()]
        def launcherArgs = createArgs(launcher, args)

        def currentProcess = launcherArgs.execute(envs, Home.getCurrent())
        currentProcess.waitForProcessOutput()

        return currentProcess.exitValue()
    }
}
