package io.peasoup.inv

import groovy.transform.CompileStatic
import org.apache.commons.lang.RandomStringUtils

import java.nio.file.Files
import java.nio.file.Paths

@CompileStatic
class InvInvoker {

    // TODO Should be editable through options
    static File Cache = new File("./.cache")

    static void invoke(InvHandler inv, File scriptPath) {
        assert inv
        assert scriptPath

        invoke(inv, scriptPath, "undefined")
    }


    static void invoke(InvHandler inv, File scriptFile, String scm) {
        assert inv
        assert scriptFile
        assert scm

        if (!scriptFile.exists()) {
            Logger.warn "does not exists: ${scriptFile.absolutePath}"
            return
        }

        Logger.debug("file: ${scriptFile.canonicalPath}")

        String preferredClassname = (normalizeClassName(scriptFile) + "#" + randomSuffix()).toLowerCase()
        Class<Script> groovyClass = new GroovyClassLoader().parseClass(scriptFile.text, cache(scriptFile, preferredClassname))

        Script myNewScript = (Script)groovyClass.newInstance()

        myNewScript.binding.setProperty("inv", inv)
        myNewScript.binding.setProperty("\$0", scriptFile.canonicalPath)
        myNewScript.binding.setProperty("pwd", scriptFile.parentFile.canonicalPath)

        if (scm)
            myNewScript.binding.setProperty("scm", scm)

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

    protected static String randomSuffix() {
        return RandomStringUtils.random(9, true, true)
    }
}
