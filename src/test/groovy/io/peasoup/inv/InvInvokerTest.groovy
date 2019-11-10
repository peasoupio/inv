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
        InvInvoker.invoke(new InvHandler(), scriptFile)

        def scriptPath = script.path
        InvInvoker.invoke(new InvHandler(), scriptFile.text, scriptPath, scriptPath)
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
            InvInvoker.invoke(null, "text", "filename", null)
        })

        // TODO Missing conditions here

    }
}