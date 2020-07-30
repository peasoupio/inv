package io.peasoup.inv.testing

import org.junit.Test

class JunitRunnerTest {

    @Test
    void ok() {
        def testScript = JunitRunnerTest.class.getResource("/inv-test-script.groovy")
        assert testScript

        def runner = new JunitRunner()
        runner.add(testScript.path)

        runner.run()
    }

}
