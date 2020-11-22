package io.peasoup.inv.repo

import io.peasoup.inv.Logger
import io.peasoup.inv.TempHome
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.jupiter.api.Assertions.*

@RunWith(TempHome.class)
class RepoInvokerTest {

    RepoExecutor repoExecutor

    @BeforeClass
    static void init() {
        RepoInvoker.newCache()
    }

    @Before
    void setup() {
        repoExecutor = new RepoExecutor()
    }

    @Test
    void ok() {
        def repoFile =  new File(TempHome.testResources,  '/repo.groovy')
        def executor = new RepoExecutor()
        executor.parse(repoFile)

        def reports = executor.execute()

        RepoExecutor.RepoHookExecutionReport report = reports.find { it.name == "my-repository" }

        assertNotNull report
    }

    @Test
    void parameters_can_be_null() {
        def repoFile =  new File(getClass().getResource('/repo-multiple.groovy').toURI())

        // parametersFile is nullable
        RepoInvoker.invoke(new RepoExecutor(), repoFile, null)
    }

    @Test
    void not_existing_script() {
        def repoFile =  new File('/repo-does-not-exists.groovy')

        def exec = new RepoExecutor()
        RepoInvoker.invoke(exec, repoFile, null)

        assertTrue exec.repos.isEmpty()
    }

    @Test
    void multiple() {

        def repoFile =  new File(getClass().getResource('/repo-multiple.groovy').toURI())
        def executor = new RepoExecutor()
        executor.parse(repoFile)

        def reports = executor.execute()

        assertNotNull reports.find { it.name == "my-first-repository" }
        assertNotNull reports.find { it.name =="my-second-repository" }
        assertNotNull reports.find { it.name =="my-third-repository" }
    }

    @Test
    void invalid() {

        def repoFile =  new File(getClass().getResource('/repo-invalid.groovy').toURI())
        def executor = new RepoExecutor()
        executor.parse(repoFile)

        def report = executor.execute()
        assertFalse report.any { it.isOk() }
    }

    @Test
    void debug_ok() {
        def logs = Logger.capture(new LinkedList())

        def repoFile =  new File(TempHome.testResources,  '/repo-debug.groovy')
        def executor = new RepoExecutor()

        executor.parse(repoFile)

        assertTrue logs.any { it == "ok" }

        Logger.capture(null)
    }

    @Test
    void invoke_not_ok() {

        // inv invoker is null
        assertThrows(IllegalArgumentException.class, {
            RepoInvoker.invoke(null, null)
        })

        // script file is null
        assertThrows(IllegalArgumentException.class, {
            RepoInvoker.invoke(new RepoExecutor(), null)
        })

        // parameters file is null
        assertThrows(IllegalArgumentException.class, {
            RepoInvoker.expectedParametersfileLocation((File)null)
        })

        // parameters file is null
        assertThrows(IllegalArgumentException.class, {
            RepoInvoker.expectedParametersfileLocation((String)null)
        })

    }
}