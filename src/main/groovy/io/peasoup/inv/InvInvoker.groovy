package io.peasoup.inv

class InvInvoker {

    static void invoke(InvDescriptor inv, File scriptPath) {
        assert inv
        assert scriptPath

        def scriptFile = scriptPath.absolutePath.replace("\\", "/")

        invoke(inv, scriptPath.text, scriptFile, scriptFile)
    }

    static void invoke(InvDescriptor inv, File scriptPath, String className) {
        assert inv
        assert scriptPath

        def scriptFile = scriptPath.absolutePath.replace("\\", "/")

        invoke(inv, scriptPath.text, scriptFile, className)
    }

    static void invoke(InvDescriptor inv, String text, String filepath, String filename) {
        assert inv
        assert text
        assert filename

        Class<Script> groovyClass = new GroovyClassLoader().parseClass(text, filename)

        Script myNewScript = (Script)groovyClass.newInstance()

        myNewScript.binding.setProperty("inv", inv)
        myNewScript.binding.setProperty("pwd", new File(filepath).parentFile.absolutePath)

        myNewScript.run()
    }

}
