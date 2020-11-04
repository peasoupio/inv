package io.peasoup.inv.loader

import io.peasoup.inv.TempHome
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.jupiter.api.Assertions.*

@RunWith(TempHome.class)
class GroovyLoaderTest {

    GroovyLoader loader
    GroovyLoader securedLoader

    @Before
    void setup() {
        loader = new GroovyLoader(false, true, null, null)
        securedLoader = new GroovyLoader(true, false, null, null)
    }

    @Test
    void ok() {
        assertNotNull securedLoader.parseClassText('''
println "Hello world!"
''')
    }

    @Test
    void insecure_system() {
        assertNull securedLoader.parseClassText('''
System.lineSeparator()
''')
    }

    @Test
    void insecure_thread() {
        assertNull securedLoader.parseClassText('''
Thread.currentThread()
''')
        
    }

    @Test
    void insecure_eval() {
        assertNull securedLoader.parseClassText('''
Eval.me('1+1')
''')
    }

    @Test
    void parseClass_with_package_ok() {
        def script1 = new File(TempHome.testResources, "/groovyloader-package-1.groovy")
        def script2 = new File(TempHome.testResources, "/groovyloader-package-2.groovy")
        def script3 = new File(TempHome.testResources, "/groovyloader-package-3.groovy")
        def script4 = new File(TempHome.testResources, "/groovyloader-package-4.groovy")

        loader.parseClassFile(script3, "org.test.classes")
        loader.parseClassFile(script4, "org.test.classes")

        loader.parseScriptFile(script1, "org.test.classes").run()
        loader.parseScriptFile(script2, "org.test.other.classes").run() // requires an "import"
    }

    @Test
    void normalize() {
        assertEquals "test", securedLoader.normalizeGroovyFilename(new File("test.groovy"))
        assertEquals "parent", securedLoader.normalizeGroovyFilename(new File("parent", "inv"))
        assertEquals "parent", securedLoader.normalizeGroovyFilename(new File("parent", "inv.groovy"))
        assertEquals "parent", securedLoader.normalizeGroovyFilename(new File("parent" ,"inv.groovy"))
    }
}
