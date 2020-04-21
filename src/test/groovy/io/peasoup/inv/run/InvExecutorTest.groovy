package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

class InvExecutorTest {

    InvExecutor invExecutor

    @Before
    void setup() {
        invExecutor = new InvExecutor()
    }

    @Test
    void raising_exception_ready() {

        def ctx = new Inv.Context(invExecutor.pool)
        def inv = ctx.build()

        inv.name = "my-index"
        inv.ready = { throw new Exception("raising_exception_ready") }
        inv.dumpDelegate()

        invExecutor.execute()
    }
}
