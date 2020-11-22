package io.peasoup.inv.repo

import io.peasoup.inv.Home
import io.peasoup.inv.TempHome
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull

@RunWith(TempHome.class)
class RepoExecutorTest {

    RepoExecutor repoExecutor

    @Before
    void setup() {
        repoExecutor = new RepoExecutor()
    }

    @Test
    void hooks_undefined() {
        RepoDescriptor desc = new RepoDescriptor(new File(Home.getCurrent(), "dummy-script.yml"))
        desc.with {
            name("my-name")
            path("./target/test-classes/exitValue")
        }

        repoExecutor.add(desc)
        def report = repoExecutor.execute()

        assertNotNull report
        assertFalse report.isEmpty()
        assertFalse report[0].isOk()
    }

    @Test
    void hooks_empty() {
        RepoDescriptor desc = new RepoDescriptor(new File(Home.getCurrent(), "dummy-script.yml"))
        desc.with {
            name("my-name")
            path("./target/test-classes/exitValue")
            hooks {
            }
        }

        repoExecutor.add(desc)
        def report = repoExecutor.execute()

        assertNotNull report
        assertFalse report.isEmpty()
        assertFalse report[0].isOk()
    }

    @Test
    void hook_exitValue() {
        RepoDescriptor desc = new RepoDescriptor(new File(Home.getCurrent(), "dummy-script.yml"))
        desc.with {
            name("my-name")
            path("./target/test-classes/exitValue")
            hooks {
                init "exit 1"
            }
        }

        repoExecutor.add(desc)
        def report = repoExecutor.execute()

        assertNotNull report
        assertFalse report.isEmpty()
        assertFalse report[0].isOk()
    }
}
