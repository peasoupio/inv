package io.peasoup.inv.composer

import org.eclipse.jetty.websocket.api.Session
import org.junit.Test

import static org.junit.Assert.*

class BootTest {

    @Test
    void messageStreamer_ok() {
        def messageStreamer = new Boot.MessageStreamer()

        assertNotNull messageStreamer.sessions
        assertTrue messageStreamer.sessions.isEmpty()
    }

    @Test
    void messageStreamer_connected() {
        def messageStreamer = new Boot.MessageStreamer()
        messageStreamer.connected(new Object() as Session)

        assertFalse messageStreamer.sessions.isEmpty()
    }

    @Test
    void messageStreamer_closed() {
        def messageStreamer = new Boot.MessageStreamer()
        def session = new Object() as Session

        messageStreamer.sessions.clear()
        messageStreamer.connected(session)
        messageStreamer.closed(session, 0, "reason")

        assertTrue messageStreamer.sessions.isEmpty()
    }

}
