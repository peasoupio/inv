package io.peasoup.inv.composer

import io.peasoup.inv.run.LogRoller
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class ExecutionTest {

    String base = "./target/test-classes"

    @Test
    void ok_alreadyExists() {
        def totalLines = Execution.MESSAGES_STATIC_CLUSTER_SIZE * 2

        def executionLog = new File(LogRoller.latest.folder(), "run.txt")
        executionLog.delete()
        executionLog << (1..totalLines).collect { "myLog#${it}\n" }.join()

        def execution = new Execution(new File(base, "scms/"), new File(base, "params/"))

        assert !execution.messages.isEmpty()
        assert execution.messages.max { it.size() }.size() == Execution.MESSAGES_STATIC_CLUSTER_SIZE

        executionLog.delete()
    }

    @Test
    void not_ok() {

        assertThrows(AssertionError.class, {
            new Execution(null, new File(base, "params/"))
        })

        assertThrows(AssertionError.class, {
            new Execution(new File(base, "scms/"), null)
        })
    }
}
