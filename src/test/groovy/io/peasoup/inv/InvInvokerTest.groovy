package io.peasoup.inv

import io.peasoup.inv.utils.Stdout
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class InvInvokerTest {

    @Test
    void invoke() {
        Logger.DebugModeEnabled = true

        def script = InvInvokerTest.class.getResource("/invokerTestScript.groovy")
        assert script

        def scriptFile = new File(script.path)

        // Resolve with filename as classname
        Stdout.capture ({ InvInvoker.invoke(new InvHandler(), scriptFile) }, {
            assert it.contains("From invokertestscript")
        })
    }

    @Test
    void cache() {

        def script = InvInvokerTest.class.getResource("/invokerTestScript.groovy")
        assert script

        InvInvoker.Cache.deleteDir()

        InvInvoker.cache(new File(script.path), "my-class")

        assert InvInvoker.Cache.exists()
        assert new File(InvInvoker.Cache, "my-class.groovy").exists()
    }

    @Test
    void normalize() {
        assert InvInvoker.normalizeClassName(new File("test.groovy")) == "test"
        assert InvInvoker.normalizeClassName(new File("parent", "inv")) == "parent"
        assert InvInvoker.normalizeClassName(new File("parent", "inv.groovy")) == "parent"
        assert InvInvoker.normalizeClassName(new File("parent" ,"inv.groovy")) == "parent"
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
            InvInvoker.invoke(null, null, null, null)
        })

        assertThrows(PowerAssertionError.class, {
            InvInvoker.invoke(new InvHandler(), null, null, null)
        })

        assertThrows(PowerAssertionError.class, {
            InvInvoker.invoke(new InvHandler(), 'pwd', null, null)
        })

        assertThrows(PowerAssertionError.class, {
            InvInvoker.invoke(new InvHandler(), 'pwd', new File("filename"), null)
        })
    }
}