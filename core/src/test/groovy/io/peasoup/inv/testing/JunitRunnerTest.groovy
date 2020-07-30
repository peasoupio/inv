package io.peasoup.inv.testing

import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class JunitRunnerTest {

    @Test
    void ok() {
        def testScript = JunitRunnerTest.class.getResource("/inv-test-script.groovy")
        assert testScript

        def runner = new JunitRunner()
        runner.add(testScript.path)

        assertTrue runner.run()
    }

    @Test
    void load_failed() {
        def testScript = JunitRunnerTest.class.getResource("/inv-test-script-failed.groovy")
        assert testScript

        def runner = new JunitRunner()
        runner.add(testScript.path)

        assertFalse runner.run()
    }

}
