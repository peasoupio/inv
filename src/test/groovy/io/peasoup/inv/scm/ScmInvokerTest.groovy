package io.peasoup.inv.scm


import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class ScmInvokerTest {

    ScmExecutor scmExecutor
    ScmHandler scmHandler

    @Before
    void setup() {
        scmExecutor = new ScmExecutor()
        scmHandler = new ScmHandler(scmExecutor)
    }

    @Test
    void ok() {

        def scmFile =  new File(getClass().getResource('/scm.groovy').toURI())
        def executor = new ScmExecutor()
        executor.read(scmFile)

        def files = executor.execute()

        assert files["my-repository"]
        assert files["my-repository"].entry.contains("mainTestScript.groovy")
        assert files["my-repository"].path.absolutePath.contains("scm")
    }

    @Test
    void not_ok() {

        def scmFile =  new File(getClass().getResource('/scm-multiple.groovy').toURI())

        assertThrows(PowerAssertionError.class, {
            ScmInvoker.invoke(null, scmFile)
        })

        assertThrows(PowerAssertionError.class, {
            ScmInvoker.invoke(scmHandler, null)
        })

        assertThrows(PowerAssertionError.class, {
            ScmInvoker.invoke(scmHandler, new File("not-existing"))
        })
    }

    @Test
    void multiple() {

        def scmFile =  new File(getClass().getResource('/scm-multiple.groovy').toURI())
        def executor = new ScmExecutor()
        executor.read(scmFile)

        def files = executor.execute()

        assert files["my-first-repository"]
        assert files["my-second-repository"]
        assert files["my-third-repository"]
    }
}