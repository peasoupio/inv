package io.peasoup.inv.scm

import groovy.transform.CompileStatic
import io.peasoup.inv.run.Logger
import io.peasoup.inv.security.CommonLoader

@CompileStatic
class ScmInvoker {

    private final static CommonLoader loader = new CommonLoader()

    private ScmInvoker() {}

    static void invoke(ScmHandler scmHandler, File scmFile) {
        assert scmHandler, 'SCM handler is required'
        assert scmFile, 'SCM file is required'

        if (!scmFile.exists()) {
            Logger.warn "SCM file does not exists: ${scmFile.absolutePath}"
            return
        }

        Script myNewScript = loader.parseClass(scmFile)
        if (!myNewScript)
            return

        myNewScript.binding.setProperty("scm", scmHandler)
        myNewScript.run()
    }
}