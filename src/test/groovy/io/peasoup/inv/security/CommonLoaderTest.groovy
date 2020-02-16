package io.peasoup.inv.security

import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class CommonLoaderTest {

    CommonLoader loader

    @Before
    void setup() {
        loader = new CommonLoader()
    }

    @Test
    void ok() {
        loader.parseClass('''
println "Hello world!"
''')
    }

    @Test
    void insecure_package() {
        assertThrows(MultipleCompilationErrorsException.class, {
            loader.parseClass('''
package io.peasoup.inv
''')
        })
    }

    @Test
    void insecure_system() {
        assertThrows(MultipleCompilationErrorsException.class, {
            loader.parseClass('''
System.lineSeparator()
''')
        })
    }

    @Test
    void insecure_thread() {
        assertThrows(MultipleCompilationErrorsException.class, {
            loader.parseClass('''
Thread.currentThread()
''')
        })
    }

    @Test
    void insecure_eval() {
        assertThrows(MultipleCompilationErrorsException.class, {
            loader.parseClass('''
Eval.me('1+1')
''')
        })
    }
}
