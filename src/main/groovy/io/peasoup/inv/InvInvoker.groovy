package io.peasoup.inv

class InvInvoker {

    static void invoke(InvHandler inv, File scriptPath) {
        assert inv
        assert scriptPath

        def scriptFile = scriptPath.canonicalPath

        invoke(inv, scriptPath.text, scriptFile, scriptFile)
    }

    static void invoke(InvHandler inv, File scriptPath, String className) {
        assert inv
        assert scriptPath

        invoke(inv, scriptPath.text, scriptPath.canonicalPath, className)
    }

    static void invoke(InvHandler inv, String text, String filepath, String filename) {
        assert inv
        assert text
        assert filename

        Logger.info("file: ${filepath}")

        Class<Script> groovyClass = new GroovyClassLoader().parseClass(text, filename)

        Script myNewScript = (Script)groovyClass.newInstance()

        myNewScript.binding.setProperty("inv", inv)
        myNewScript.binding.setProperty("pwd", new File(filepath).parentFile.canonicalPath)

        myNewScript.run()
    }

}
