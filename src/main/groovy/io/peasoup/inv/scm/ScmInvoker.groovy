package io.peasoup.inv.scm

class ScmInvoker {

    private ScmInvoker() {}

    static void invoke(ScmHandler scmHandler, File scmFile) {
        assert scmFile != null

        Class<Script> groovyClass = new GroovyClassLoader().parseClass(scmFile)
        Script myNewScript = (Script)groovyClass.newInstance()

        myNewScript.binding.setProperty("scm", scmHandler)

        myNewScript.run()
    }
}