package io.peasoup.inv

class InvInvoker {

    static void invoke(InvDescriptor inv, File scriptPath) {
        assert inv
        assert scriptPath

        invoke(inv, scriptPath.text, scriptPath.absolutePath.replace("\\", "/"))

    }
    static void invoke(InvDescriptor inv, String text, String filename) {
        assert inv
        assert text
        assert filename

        Class<Script> groovyClass = new GroovyClassLoader().parseClass(text, filename)

        Script myNewScript = (Script)groovyClass.newInstance()

        myNewScript.binding.setProperty("inv", inv)
        myNewScript.binding.setProperty("pwd", new File(filename).parentFile.absolutePath)

        myNewScript.run()
    }

}
