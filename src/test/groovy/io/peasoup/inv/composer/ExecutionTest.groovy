package io.peasoup.inv.composer

import groovy.mock.interceptor.MockFor
import io.peasoup.inv.TempHome
import io.peasoup.inv.run.RunsRoller
import org.eclipse.jetty.websocket.api.Session
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.jupiter.api.Assertions.assertThrows

@RunWith(TempHome.class)
class ExecutionTest {

    String base = "./target/test-classes"

    @Test
    void ok_alreadyExists() {
        RunsRoller.latest.roll() // make sure to roll once

        def totalLines = Execution.MESSAGES_STATIC_CLUSTER_SIZE * 2

        def executionLog = new File(RunsRoller.latest.folder(), "run.txt")
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

    @Test
    void messageStreamer() {
        assert Execution.MessageStreamer.sessions != null
        assert Execution.MessageStreamer.sessions.isEmpty()

        Session sessionMock = new MockFor(Session).proxy as Session

        def messageStreamer = new Execution.MessageStreamer()

        messageStreamer.connected(sessionMock)
        assert Execution.MessageStreamer.sessions.contains(sessionMock)

        messageStreamer.closed(sessionMock, 0, null)
        assert !Execution.MessageStreamer.sessions.contains(sessionMock)
    }
}
