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
        def inv = new InvHandler(new InvExecutor())

        Stdout.capture ({ InvInvoker.invoke(inv, scriptFile) }, {
            assert it.contains("inv-invoker-script.groovy")
        })
    }

    @Test
    void cache() {

        def script = InvInvokerTest.class.getResource("/inv-invoker-script.groovy")
        assert script

        // Clean if already existing
        def scriptFolder = new File(RunsRoller.latest.folder(), "scripts/")
        scriptFolder.deleteDir()

        assert !scriptFolder.exists()

        InvInvoker.cache(new File(script.path), "my-class")

        assert scriptFolder.exists()
        assert new File(scriptFolder, "my-class.groovy").exists()
    }

    @Test
    void debug_ok() {
        def logs = Logger.capture(new LinkedList())

        def script = InvInvokerTest.class.getResource("/inv-invoker-script-with-debug.groovy")
        assert script

        def scriptFile = new File(script.path)
        def inv = new InvHandler(new InvExecutor())

        InvInvoker.invoke(inv, scriptFile)

        assert logs.any { it == "ok" }

        Logger.capture(null)
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

        assertThrows(IllegalArgumentException.class, {
            InvInvoker.invoke(null, null)
        })

        assertThrows(IllegalArgumentException.class, {
            InvInvoker.invoke(new InvHandler(), null)
        })

        assertThrows(IllegalArgumentException.class, {
            InvInvoker.invoke(null, null, null, null)
        })

        assertThrows(IllegalArgumentException.class, {
            InvInvoker.invoke(new InvHandler(), null, null, null)
        })

        assertThrows(IllegalArgumentException.class, {
            InvInvoker.invoke(new InvHandler(), null, 'pwd', null)
        })

        assertThrows(IllegalArgumentException.class, {
            InvInvoker.invoke(new InvHandler(), new File("filename"), 'pwd', null)
        })
    }
}