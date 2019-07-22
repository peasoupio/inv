package io.peasoup.inv

import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class InvInvokerTest {

    @Test
    void invoke() {
        def script = InvInvokerTest.class.getResource("/invokerTestScript.groovy")

        assert script

        def scriptFile = new File(script.path)

        ExpandoMetaClass.enableGlobally()
        InvInvoker.invoke(new InvDescriptor(), scriptFile)

        def scriptPath = script.path
        InvInvoker.invoke(new InvDescriptor(), scriptFile.text, scriptPath)
    }

    @Test
    void invoke_not_ok() {

        assertThrows(PowerAssertionError.class, {
            InvInvoker.invoke(null, null)
        })

        assertThrows(PowerAssertionError.class, {
            InvInvoker.invoke(new InvDescriptor(), null)
        })

        /*
        assertThrows(PowerAssertionError.class, {
            InvInvoker.invoke(null, "my-path")
        })
        */

        assertThrows(PowerAssertionError.class, {
            InvInvoker.invoke(null, null, null)
        })

        assertThrows(PowerAssertionError.class, {
            InvInvoker.invoke(new InvDescriptor(), null, "filename")
        })

        assertThrows(PowerAssertionError.class, {
            InvInvoker.invoke(new InvDescriptor(), "text", null)
        })

        assertThrows(PowerAssertionError.class, {
            InvInvoker.invoke(null, "text", "filename")
        })

    }
}