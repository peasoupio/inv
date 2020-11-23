package io.peasoup.inv.run

import io.peasoup.inv.Logger
import io.peasoup.inv.utils.Stdout
import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.*

class InvInvokerTest {

    InvInvoker invInvoker
    InvExecutor invExecutor

    @Before
    void setup() {
        invExecutor = new InvExecutor()
        invInvoker = invExecutor.invInvoker
    }

    @Test
    void invoke() {

        def script = InvInvokerTest.class.getResource("/inv-invoker-script.groovy")
        assertNotNull script

        def scriptFile = new File(script.path)

        Stdout.capture ({ invInvoker.invokeScript(scriptFile) }, {
            assertTrue it.contains("inv-invoker-script.groovy")
        })
    }

    @Test
    void invoke_standalone() {
        def script = InvInvokerTest.class.getResource("/inv-invoker-standalone.groovy")
        assertNotNull script

        def scriptFile = new File(script.path)
        assertTrue scriptFile.exists()

        def scriptObj = (Script) new GroovyClassLoader().parseClass(scriptFile).getDeclaredConstructor().newInstance()
        assertNotNull scriptObj

        scriptObj.run()
    }



    @Test
    void debug_ok() {
        def logs = Logger.capture(new LinkedList())

        def script = InvInvokerTest.class.getResource("/inv-invoker-script-with-debug.groovy")
        assertNotNull script

        def scriptFile = new File(script.path)

        invInvoker.invokeScript(scriptFile)

        assertTrue logs.any { it == "ok" }

        Logger.capture(null)
    }

    @Test
    void not_existing_script() {
        def repoFile =  new File('/repo-does-not-exists.groovy')

        invInvoker.invokeScript(repoFile)

        assertTrue invExecutor.pool.isEmpty()
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
            def script = InvInvokerTest.class.getResource("/inv-invoker-script-with-debug.groovy")
            invInvoker.invokeScript(new File(script.path), null, null, null)
        })

    }
}