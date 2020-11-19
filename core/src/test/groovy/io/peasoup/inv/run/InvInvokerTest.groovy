package io.peasoup.inv.run

import io.peasoup.inv.Logger
import io.peasoup.inv.utils.Stdout
import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.*

class InvInvokerTest {

    @Before
    void setup(){
        InvInvoker.newCache()
    }

    @Test
    void invoke() {

        def script = InvInvokerTest.class.getResource("/inv-invoker-script.groovy")
        assertNotNull script

        def scriptFile = new File(script.path)

        Stdout.capture ({ InvInvoker.invoke(new InvExecutor(), scriptFile) }, {
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

        InvInvoker.invoke(new InvExecutor(), scriptFile)

        assertTrue logs.any { it == "ok" }

        Logger.capture(null)
    }

    @Test
    void not_existing_script() {
        def repoFile =  new File('/repo-does-not-exists.groovy')

        def exec = new InvExecutor()
        InvInvoker.invoke(exec, repoFile)

        assertTrue exec.pool.isEmpty()
    }


    @Test
    void invoke_not_ok() {

        // inv invoker is null
        assertThrows(IllegalArgumentException.class, {
            InvInvoker.invoke(null, null)
        })

        // script file is null
        assertThrows(IllegalArgumentException.class, {
            InvInvoker.invoke(new InvExecutor(), null)
        })

        // pwd is null
        assertThrows(IllegalArgumentException.class, {
            def script = InvInvokerTest.class.getResource("/inv-invoker-script-with-debug.groovy")
            InvInvoker.invoke(new InvExecutor(), new File(script.path), null, null, null)
        })

    }
}