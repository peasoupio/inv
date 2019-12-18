package io.peasoup.inv.scm

import org.junit.Test

class ScmReaderTest {

    @Test
    void ok() {

        def scmFile =  new File(getClass().getResource('/test-scm.groovy').toURI())
        def files = new ScmReader(scmFile).execute()

        assert files["my-repository"]
        assert files["my-repository"].entry.contains("mainTestScript.groovy")
        assert files["my-repository"].path.absolutePath.contains("scm")
    }

    @Test
    void multiple() {

        def scmFile =  new File(getClass().getResource('/test-scm-multiple.groovy').toURI())
        def files = new ScmReader(scmFile).execute()

        assert files["my-first-repository"]
        assert files["my-second-repository"]
        assert files["my-third-repository"]
    }
}