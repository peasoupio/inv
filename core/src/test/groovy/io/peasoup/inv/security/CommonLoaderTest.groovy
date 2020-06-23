package io.peasoup.inv.security

import io.peasoup.inv.run.InvInvoker
import io.peasoup.inv.run.RunsRoller
import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertTrue

class CommonLoaderTest {

    CommonLoader loader

    @Before
    void setup() {
        loader = new CommonLoader(true)
    }

    @Test
    void ok() {
        assertTrue loader.compile('''
println "Hello world!"
''')
    }

    @Test
    void insecure_package() {
        assertFalse loader.compile('''
package io.peasoup.inv
''')
    }

    @Test
    void insecure_system() {
        assertFalse loader.compile('''
System.lineSeparator()
''')
    }

    @Test
    void insecure_thread() {
        assertFalse loader.compile('''
Thread.currentThread()
''')
    }

    @Test
    void insecure_eval() {
        assertFalse loader.compile('''
Eval.me('1+1')
''')
    }

    @Test
    void cache() {

        def script = CommonLoaderTest.class.getResource("/inv-invoker-script.groovy")
        assert script

        // Clean if already existing
        def scriptFolder = new File(RunsRoller.latest.folder(), "scripts/")
        scriptFolder.deleteDir()

        assert !scriptFolder.exists()

        loader.cache(new File(script.path), "my-class")

        assert scriptFolder.exists()
        assert new File(scriptFolder, "my-class.groovy").exists()
    }

    @Test
    void normalize() {
        assert loader.normalizeClassName(new File("test.groovy")) == "test"
        assert loader.normalizeClassName(new File("parent", "inv")) == "parent"
        assert loader.normalizeClassName(new File("parent", "inv.groovy")) == "parent"
        assert loader.normalizeClassName(new File("parent" ,"inv.groovy")) == "parent"
    }
}
