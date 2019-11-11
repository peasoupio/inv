package io.peasoup.inv

import java.nio.file.Files
import java.nio.file.Paths

class InvInvoker {

    static String Cache = "./.cache"

    static void invoke(InvHandler inv, File scriptPath) {
        assert inv
        assert scriptPath

        invoke(inv, scriptPath.text, scriptPath, normalizeClassName(scriptPath))
    }


    static void invoke(InvHandler inv, String text, File scriptFile, String classname) {
        assert inv
        assert text
        assert classname

        Logger.info("file: ${scriptFile.canonicalPath}")

        Class<Script> groovyClass = new GroovyClassLoader().parseClass(text, cache(scriptFile, classname))

        Script myNewScript = (Script)groovyClass.newInstance()

        myNewScript.binding.setProperty("inv", inv)
        myNewScript.binding.setProperty("pwd", scriptFile.parentFile.canonicalPath)

        myNewScript.run()
    }

    static String cache(File scriptFile, String classname) {

        File filename = new File(Cache, classname + ".groovy")
        filename.mkdirs()

        // Make sure we got latest
        Files.delete(Paths.get(filename.absolutePath))

        // Create a symlink to have dynamic updates adn save space
        Files.createSymbolicLink(Paths.get(filename.absolutePath), Paths.get(scriptFile.absolutePath))

        Logger.debug "created symlink for ${classname} here: ${filename.absolutePath}"

        return filename.absolutePath
    }

    private static String normalizeClassName(File script) {
        if (!script.parent)
            return script.name.split("\\.")[0]

        if (script.name.toLowerCase() == "inv")
            return script.parentFile.name

        if (script.name.toLowerCase() == "inv.groovy")
            return script.parentFile.name

        return script.name.split("\\.")[0]
    }

}
