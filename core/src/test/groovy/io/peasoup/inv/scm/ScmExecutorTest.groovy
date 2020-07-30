package io.peasoup.inv.scm

import org.junit.Before
import org.junit.Test

class ScmExecutorTest {

    ScmExecutor scmExecutor
    ScmHandler scmHandler

    @Before
    void setup() {
        scmExecutor = new ScmExecutor()
        scmHandler = new ScmHandler(scmExecutor)
    }

    @Test
    void hooks_undefined() {
        ScmDescriptor desc = new ScmDescriptor()
        desc.with {
            name("my-name")
            path("./target/test-classes/exitValue")
        }

        scmExecutor.add(desc)
        def report = scmExecutor.execute()

        assert report
        assert report.size() == 1
        assert !report[0].isOk
    }

    @Test
    void hooks_empty() {
        ScmDescriptor desc = new ScmDescriptor()
        desc.with {
            name("my-name")
            path("./target/test-classes/exitValue")
            hooks {
            }
        }

        scmExecutor.add(desc)
        def report = scmExecutor.execute()

        assert report
        assert report.size() == 1
        assert !report[0].isOk
    }

    @Test
    void hook_exitValue() {
        ScmDescriptor desc = new ScmDescriptor()
        desc.with {
            name("my-name")
            path("./target/test-classes/exitValue")
            hooks {
                init "exit 1"
            }
        }

        scmExecutor.add(desc)
        def report = scmExecutor.execute()

        assert report
        assert report.size() == 1
        assert !report[0].isOk
    }
}
