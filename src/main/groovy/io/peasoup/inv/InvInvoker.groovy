package io.peasoup.inv

class InvInvoker {

    static def invoke(InvDescriptor inv, String scriptPath) {

        File scriptFile = new File(scriptPath.replace("\\", "/"))

        Class<Script> groovyClass = new GroovyClassLoader().parseClass(scriptFile.text, scriptFile.absolutePath)
        Script myNewScript = groovyClass.newInstance()

        myNewScript.binding.setProperty("inv", inv)

        myNewScript.run()
    }

}
