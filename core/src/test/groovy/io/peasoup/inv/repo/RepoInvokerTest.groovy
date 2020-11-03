package io.peasoup.inv.repo

import io.peasoup.inv.TempHome
import io.peasoup.inv.run.Logger
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.jupiter.api.Assertions.*

@RunWith(TempHome.class)
class RepoInvokerTest {

    RepoExecutor repoExecutor
    RepoHandler repoHandler

    @Before
    void setup() {
        repoExecutor = new RepoExecutor()
        repoHandler = new RepoHandler(repoExecutor)
    }

    @Test
    void ok() {

        def repoFile =  new File(TempHome.testResources,  '/repo.groovy')
        def executor = new RepoExecutor()
        executor.parse(repoFile)

        def reports = executor.execute()

        RepoExecutor.RepoExecutionReport report = reports.find { it.name == "my-repository" }

        assertNotNull report
    }

    @Test
    void not_ok() {

        def repoFile =  new File(getClass().getResource('/repo-multiple.groovy').toURI())

        assertThrows(IllegalArgumentException.class, {
            RepoInvoker.invoke(null, repoFile)
        })

        assertThrows(IllegalArgumentException.class, {
            RepoInvoker.invoke(new RepoExecutor(), null)
        })

        // parametersFile is nullable
        RepoInvoker.invoke(new RepoExecutor(), repoFile, null)
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
        assertFalse report.any { it.isOk }
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
}