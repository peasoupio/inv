package io.peasoup.inv.run

import io.peasoup.inv.Logger
import io.peasoup.inv.utils.Stdout
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

class InvInvokerTest {

    InvInvoker invInvoker
    InvExecutor invExecutor

    @Before
    void setup() {
        invExecutor = new InvExecutor()
        invInvoker = invExecutor.invInvoker
    }

    @Test
    void invoke_not_ok() {

        // script file is null
        assertThrows(IllegalArgumentException.class, {
            invInvoker.invokeScript(null)
        })

        // inv invoker is null
        assertThrows(IllegalArgumentException.class, {
            invInvoker.invokeScript(null, null)
        })

        // pwd is null
        assertThrows(IllegalArgumentException.class, {
            invInvoker.invokeScript(new File("something"), null, null, null)
        })

    }
}