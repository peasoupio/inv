package io.peasoup.inv

import io.peasoup.inv.utils.Stdout
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class InvInvokerTest {

    @Test
    void invoke() {
        ExpandoMetaClass.enableGlobally()
        Logger.DebugModeEnabled = true

        def script = InvInvokerTest.class.getResource("/invokerTestScript.groovy")
        assert script

        def scriptFile = new File(script.path)

        // Resolve with filename as classname
        Stdout.capture ({ InvInvoker.invoke(new InvHandler(), scriptFile) }, {
            assert it.contains("From invokerTestScript")
        })

        // Resolve with specified classname
        Stdout.capture ({ InvInvoker.invoke(new InvHandler(), scriptFile.text, scriptFile,  "my-classname") }, {
            assert it.contains("From my-classname")
        })

    }

    @Test
    void invoke_not_ok() {

        assertThrows(PowerAssertionError.class, {
            InvInvoker.invoke(null, null)
        })

        assertThrows(PowerAssertionError.class, {
            InvInvoker.invoke(new InvHandler(), null)
        })

        assertThrows(PowerAssertionError.class, {
            InvInvoker.invoke(new InvHandler(), "text", null, null)
        })

        assertThrows(PowerAssertionError.class, {
            InvInvoker.invoke(null, "text", new File("filename"), null)
        })
    }
}