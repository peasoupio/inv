package io.peasoup.inv.loader


import io.peasoup.inv.run.RunsRoller
import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertTrue

class GroovyLoaderTest {

    GroovyLoader loader

    @Before
    void setup() {
        loader = new GroovyLoader(true)
    }

    @Test
    void ok() {
        assertNotNull loader.parseText('''
println "Hello world!"
''')
    }

    @Test
    void insecure_package() {
        assertNull loader.parseText('''
package io.peasoup.inv
''')
    }

    @Test
    void insecure_system() {
        assertNull loader.parseText('''
System.lineSeparator()
''')
    }

    @Test
    void insecure_thread() {
        assertNull loader.parseText('''
Thread.currentThread()
''')
    }

    @Test
    void insecure_eval() {
        assertNull loader.parseText('''
Eval.me('1+1')
''')
    }

    @Test
    void cache() {

        def script = GroovyLoaderTest.class.getResource("/inv-invoker-script.groovy")
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
