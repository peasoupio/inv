package io.peasoup.inv.scm

import io.peasoup.inv.TempHome
import io.peasoup.inv.run.Logger
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.jupiter.api.Assertions.assertThrows

@RunWith(TempHome.class)
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

        def scmFile =  new File(TempHome.testResources,  '/scm.groovy')
        def executor = new ScmExecutor()
        executor.read(scmFile)

        def reports = executor.execute()

        ScmExecutor.SCMExecutionReport report = reports.find { it.name == "my-repository" }

        assert report
        assert report.repository.entry.size() == 1
        assert report.repository.entry[0].contains("mainTestScript.groovy")
        assert report.repository.path.absolutePath.contains("test-resources")
    }

    @Test
    void not_ok() {

        def scmFile =  new File(getClass().getResource('/scm-multiple.groovy').toURI())

        assertThrows(IllegalArgumentException.class, {
            ScmInvoker.invoke(null, scmFile)
        })

        assertThrows(IllegalArgumentException.class, {
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

    @Test
    void debug_ok() {
        def logs = Logger.capture(new LinkedList())

        def scmFile =  new File(TempHome.testResources,  '/scm-debug.groovy')
        def executor = new ScmExecutor()

        executor.read(scmFile)

        assert logs.any { it == "ok" }

        Logger.capture(null)
    }
}