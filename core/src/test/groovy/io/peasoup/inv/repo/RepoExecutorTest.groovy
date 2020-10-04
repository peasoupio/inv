package io.peasoup.inv.repo

import org.junit.Before
import org.junit.Test

class RepoExecutorTest {

    RepoExecutor repoExecutor
    RepoHandler repoHandler

    @Before
    void setup() {
        repoExecutor = new RepoExecutor()
        repoHandler = new RepoHandler(repoExecutor)
    }

    @Test
    void hooks_undefined() {
        RepoDescriptor desc = new RepoDescriptor()
        desc.with {
            name("my-name")
            path("./target/test-classes/exitValue")
        }

        repoExecutor.add(desc)
        def report = repoExecutor.execute()

        assert report
        assert report.size() == 1
        assert !report[0].isOk
    }

    @Test
    void hooks_empty() {
        RepoDescriptor desc = new RepoDescriptor()
        desc.with {
            name("my-name")
            path("./target/test-classes/exitValue")
            hooks {
            }
        }

        repoExecutor.add(desc)
        def report = repoExecutor.execute()

        assert report
        assert report.size() == 1
        assert !report[0].isOk
    }

    @Test
    void hook_exitValue() {
        RepoDescriptor desc = new RepoDescriptor()
        desc.with {
            name("my-name")
            path("./target/test-classes/exitValue")
            hooks {
                init "exit 1"
            }
        }

        repoExecutor.add(desc)
        def report = repoExecutor.execute()

        assert report
        assert report.size() == 1
        assert !report[0].isOk
    }
}
