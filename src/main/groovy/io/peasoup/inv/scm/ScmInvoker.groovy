package io.peasoup.inv.scm

import io.peasoup.inv.run.Logger

class ScmInvoker {

    private ScmInvoker() {}

    static void invoke(ScmHandler scmHandler, File scmFile) {
        assert scmHandler, 'SCM handler is required'
        assert scmFile, 'SCM file is required'

        if (!scmFile.exists()) {
            Logger.warn "SCM file does not exists: ${scmFile.absolutePath}"
            return
        }

        Class<Script> groovyClass = new GroovyClassLoader().parseClass(scmFile)
        Script myNewScript = (Script)groovyClass.newInstance()

        myNewScript.binding.setProperty("scm", scmHandler)

        myNewScript.run()
    }
}