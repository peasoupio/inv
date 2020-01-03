package io.peasoup.inv

import groovy.transform.CompileStatic

import javax.xml.bind.DatatypeConverter
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest

@CompileStatic
class InvInvoker {

    // TODO Should be editable through options
    static File Cache = new File("./.cache")

    static void invoke(InvHandler inv, File scriptPath) {
        assert inv
        assert scriptPath

        invoke(inv, scriptPath.parent, scriptPath, "undefined")
    }

    static void invoke(InvHandler inv, String pwd, File scriptFile, String scm, Map<String, Object> inject = [:]) {
        assert inv
        assert pwd
        assert scriptFile
        assert scm

        if (!scriptFile.exists()) {
            Logger.warn "does not exists: ${scriptFile.absolutePath}"
            return
        }

        Logger.debug("file: ${scriptFile.canonicalPath}")

        String preferredClassname = (normalizeClassName(scriptFile) + '_' + checksum(scriptFile)).toLowerCase()
        Class<Script> groovyClass = new GroovyClassLoader().parseClass(scriptFile.text, cache(scriptFile, preferredClassname))

        Script myNewScript = (Script)groovyClass.newInstance()

        myNewScript.binding.setProperty("inv", inv)
        myNewScript.binding.setProperty("\$0", scriptFile.canonicalPath)
        myNewScript.binding.setProperty("pwd", checkSubordinateSlash(pwd))

        if (scm)
            myNewScript.binding.setProperty("scm", scm)

        if (!inject.isEmpty())
            // Insert user-defined instances within the script itself
            inject.each { String className, Object instance ->
                myNewScript.binding.setProperty(className, instance)
            }

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

    private static String checksum(File path) {
        ByteArrayOutputStream baos = null
        ObjectOutputStream oos = null
        try {
            baos = new ByteArrayOutputStream()
            oos = new ObjectOutputStream(baos)
            oos.writeObject(path.absolutePath)
            MessageDigest md = MessageDigest.getInstance("MD5")
            byte[] thedigest = md.digest(baos.toByteArray())
            return DatatypeConverter.printHexBinary(thedigest)
        } finally {
            oos.close()
            baos.close()
        }
    }

    protected static String checkSubordinateSlash(String path) {
        assert path

        if (path.charAt(path.length() - 1) == '/')
            return path

        return path + '/'
    }
}
