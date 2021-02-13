package io.peasoup.inv.loader

import io.peasoup.inv.TempHome
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull

@RunWith(TempHome.class)
class GroovyLoaderTest {

    GroovyLoader loader
    GroovyLoader securedLoader

    @Before
    void setup() {
        loader = new GroovyLoader(false, null, null)
        securedLoader = new GroovyLoader(true, null, null)
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

        loader.addClassFile(script3, "org.test.classes")
        loader.addClassFile(script4, "org.test.classes")

        loader.compileClasses()

        loader.parseScriptFile(script1, "org.test.classes").run()
        loader.parseScriptFile(script2, "org.test.other.classes").run() // requires an "import"
    }
}
