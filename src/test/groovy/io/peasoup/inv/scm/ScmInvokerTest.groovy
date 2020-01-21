package io.peasoup.inv.scm


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

        def reports = executor.execute()

        ScmExecutor.SCMReport report = reports.find { it.name == "my-repository" }

        assert report
        assert report.repository.entry.size() == 1
        assert report.repository.entry[0].contains("mainTestScript.groovy")
        assert report.repository.path.absolutePath.contains("scm")
    }

    @Test
    void not_ok() {

        def scmFile =  new File(getClass().getResource('/scm-multiple.groovy').toURI())

        assertThrows(AssertionError.class, {
            ScmInvoker.invoke(null, scmFile)
        })

        assertThrows(AssertionError.class, {
            ScmInvoker.invoke(scmHandler, null)
        })
    }

    @Test
    void multiple() {

        def scmFile =  new File(getClass().getResource('/scm-multiple.groovy').toURI())
        def executor = new ScmExecutor()
        executor.read(scmFile)

        def reports = executor.execute()

        assert reports.find { it.name == "my-first-repository" }
        assert reports.find { it.name =="my-second-repository" }
        assert reports.find { it.name =="my-third-repository" }
    }

    @Test
    void invalid() {

        def scmFile =  new File(getClass().getResource('/scm-invalid.groovy').toURI())
        def executor = new ScmExecutor()
        executor.read(scmFile)

        def report = executor.execute()
        assert !report.any { it.isOk }
    }
}