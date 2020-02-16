package io.peasoup.inv.security


import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertNull

class CommonLoaderTest {

    CommonLoader loader

    @Before
    void setup() {
        loader = new CommonLoader()
    }

    @Test
    void ok() {
        assertNotNull loader.parseClass('''
println "Hello world!"
''')
    }

    @Test
    void insecure_package() {
        assertNull loader.parseClass('''
package io.peasoup.inv
''')
    }

    @Test
    void insecure_system() {
        assertNull loader.parseClass('''
System.lineSeparator()
''')
    }

    @Test
    void insecure_thread() {
        assertNull loader.parseClass('''
Thread.currentThread()
''')
    }

    @Test
    void insecure_eval() {
        assertNull loader.parseClass('''
Eval.me('1+1')
''')
    }
}
