package io.peasoup.inv.run

import io.peasoup.inv.utils.Stdout
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class InvInvokerTest {

    @Test
    void invoke() {

        def script = InvInvokerTest.class.getResource("/inv-invoker-script.groovy")
        assert script

        def scriptFile = new File(script.path)

        Stdout.capture ({ InvInvoker.invoke(new InvExecutor(), scriptFile) }, {
            assert it.contains("inv-invoker-script.groovy")
        })
    }

    @Test
    void invoke_standalone() {
        def script = InvInvokerTest.class.getResource("/inv-invoker-standalone.groovy")
        assert script

        def scriptFile = new File(script.path)
        assert scriptFile.exists()

        def scriptObj = (Script) new GroovyClassLoader().parseClass(scriptFile).getDeclaredConstructor().newInstance()
        assert scriptObj

        scriptObj.run()
    }



    @Test
    void debug_ok() {
        def logs = Logger.capture(new LinkedList())

        def script = InvInvokerTest.class.getResource("/inv-invoker-script-with-debug.groovy")
        assert script

        def scriptFile = new File(script.path)

        InvInvoker.invoke(new InvExecutor(), scriptFile)

        assert logs.any { it == "ok" }

        Logger.capture(null)
    }



    @Test
    void invoke_not_ok() {

        assertThrows(IllegalArgumentException.class, {
            InvInvoker.invoke(null, null)
        })

        assertThrows(IllegalArgumentException.class, {
            InvInvoker.invoke(new InvExecutor(), null)
        })

        assertThrows(IllegalArgumentException.class, {
            InvInvoker.invoke(null, null, null, null)
        })

        assertThrows(IllegalArgumentException.class, {
            InvInvoker.invoke(new InvExecutor(), null, null, null)
        })

        assertThrows(IllegalArgumentException.class, {
            InvInvoker.invoke(new InvExecutor(), null, 'pwd', null)
        })
    }
}