package io.peasoup.inv.web


import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class ExecutionTest {

    String base = "./target/test-classes"

    @Test
    void ok_alreadyExists() {

        def totalLines = Execution.MESSAGES_STATIC_CLUSTER_SIZE * 2

        def executions = new File(base, "executions/")
        executions.mkdir()

        def executionLog = new File(executions, "execution.log")
        executionLog.delete()
        executionLog << (1..totalLines).collect { "myLog#${it}\n" }.join()

        def execution = new Execution(executions, new File(base, "scms/"), new File(base, "params/"))

        assert !execution.messages.isEmpty()
        assert execution.messages.size() == 2

        executionLog.delete()
    }

    @Test
    void not_ok() {
        assertThrows(AssertionError.class, {
            new Execution(null, new File(base, "scms/"), new File(base, "params/"))
        })

        assertThrows(AssertionError.class, {
            new Execution(new File(base, "executions/"), null, new File(base, "params/"))
        })

        assertThrows(AssertionError.class, {
            new Execution(new File(base, "executions/"), new File(base, "scms/"), null)
        })
    }
}
