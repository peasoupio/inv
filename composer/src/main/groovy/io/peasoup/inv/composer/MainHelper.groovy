package io.peasoup.inv.composer

import io.peasoup.inv.Home

class MainHelper {

    private static final MAIN_CLASS_NAME = "io.peasoup.inv.Main"

    private MainHelper() {
        // private ctor
    }

    static List<String> createArgs(String launcher, List<String> args) {
        Class<?> mainCls = Thread.currentThread().getContextClassLoader().loadClass(MAIN_CLASS_NAME)
        if (mainCls == null)
            throw new IllegalStateException("Could not resolve Main CLI class")

        String mainLocation = mainCls.getProtectionDomain().getCodeSource().getLocation().getPath()
        // If it is from a JAR file, use the -jar option
        if (mainLocation.endsWith(".jar"))
            return ["java", "-jar", launcher] + args
        // Otherwise, use the current classpath
        else
            return ["java", "-classpath", System.getProperty("java.class.path"), MAIN_CLASS_NAME] + args
    }

    static int execute(String launcher, List<String> args) {
        def envs = System.getenv().collect { "${it.key}=${it.value}".toString() } + ["INV_HOME=${Home.getCurrent().absolutePath}".toString()]
        def launcherArgs = createArgs(launcher, args)

        def currentProcess = launcherArgs.execute(envs, Home.getCurrent())
        currentProcess.waitForProcessOutput()

        return currentProcess.exitValue()
    }
}
