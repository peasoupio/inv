package io.peasoup.inv

class InvInvoker {

    static void invoke(InvHandler inv, File scriptPath) {
        assert inv
        assert scriptPath


        invoke(inv, scriptPath.text, scriptPath.canonicalPath, normalizeClassName(scriptPath))
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

    private static String normalizeClassName(File script) {
        if (!script.parent)
            return script.name.split("\\.")[0]

        if (script.name.toLowerCase() == "inv")
            return script.parent

        if (script.name.toLowerCase() == "inv.groovy")
            return script.parent

        return script.name.split("\\.")[0]
    }

}
