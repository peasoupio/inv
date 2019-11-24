package io.peasoup.inv

import groovy.transform.CompileStatic

import java.nio.file.Files
import java.nio.file.Paths

@CompileStatic
class InvInvoker {

    // TODO Should be editable through options
    static File Cache = new File("./.cache")

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

        // Make sure cache is available with minimal accesses
        if (!Cache.exists()) {
            Cache.mkdirs()
            Cache.setExecutable(true)
            Cache.setWritable(true)
            Cache.setReadable(true)
        }

        File filename = new File(Cache, classname + ".groovy")
        filename.mkdirs()

        // Make sure we got latest
        Files.delete(Paths.get(filename.absolutePath))

        // Create a symlink to have dynamic updates adn save space
        //Files.createSymbolicLink(Paths.get(filename.absolutePath), Paths.get(scriptFile.absolutePath))
        Files.copy(Paths.get(scriptFile.absolutePath), Paths.get(filename.absolutePath))

        Logger.debug "created copy for ${classname} here: ${filename.absolutePath}"

        return filename.absolutePath
    }

    protected static String normalizeClassName(File script) {
        if (!script.parent)
            return script.name.split("\\.")[0]

        if (script.name.toLowerCase() == "inv")
            return script.parentFile.name

        if (script.name.toLowerCase() == "inv.groovy")
            return script.parentFile.name

        return script.name.split("\\.")[0]
    }

}
