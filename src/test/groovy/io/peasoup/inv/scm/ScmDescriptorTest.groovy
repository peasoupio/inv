package io.peasoup.inv.scm

import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Test

import static junit.framework.Assert.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

class ScmDescriptorTest  {

    @Test
    void ok() {

        def testScm = ScmDescriptorTest.class.getResource("/test-scm.groovy")
        def scmDescriptor = new ScmDescriptor(testScm.newReader())

        assert scmDescriptor.scms()["my-repository"]

        assertEquals scmDescriptor.scms()["my-repository"].src, "https://github.com/spring-guides/gs-spring-boot.git"
        assertEquals scmDescriptor.scms()["my-repository"].entry, new File("./src/test/resources/mainTestScript.groovy").absolutePath
        assertEquals scmDescriptor.scms()["my-repository"].timeout, 30000

        assert scmDescriptor.scms()["my-repository"].hooks
        assert scmDescriptor.scms()["my-repository"].hooks.init.contains("mkdir my-repository")
        assert scmDescriptor.scms()["my-repository"].hooks.update.contains("echo 'update'")
    }

    @Test
    void invalid_scm() {
        def scmDescriptor = new ScmDescriptor(new BufferedReader(new StringReader("""
'test' null
""")))

        assert scmDescriptor.scms().isEmpty()
    }

    @Test
    void invalid_path() {
        def scmDescriptor = new ScmDescriptor(new BufferedReader(new StringReader("""
'test' {
    path 1
}
""")))

        assertThrows(Exception.class, {
            scmDescriptor.scms()
        })
    }

    @Test
    void invalid_src() {
        def scmDescriptor = new ScmDescriptor(new BufferedReader(new StringReader("""
'test' {
    src 1
}
""")))

        assertThrows(Exception.class, {
            scmDescriptor.scms()
        })
    }

    @Test
    void invalid_entry() {
        def scmDescriptor = new ScmDescriptor(new BufferedReader(new StringReader("""
'test' {
    entry 1
}
""")))

        assertThrows(Exception.class, {
            scmDescriptor.scms()
        })
    }

    @Test
    void invalid_timeout() {
        def scmDescriptor = new ScmDescriptor(new BufferedReader(new StringReader("""
'test' {
    timeout "0"
}
""")))

        assertThrows(Exception.class, {
            scmDescriptor.scms()
        })
    }

    @Test
    void invalid_hooks() {
        def scmDescriptor = new ScmDescriptor(new BufferedReader(new StringReader("""
'test' {
    hooks null
}
""")))

        assertThrows(PowerAssertionError.class, {
            scmDescriptor.scms()
        })
    }

    @Test
    void invalid_ask() {
        def scmDescriptor = new ScmDescriptor(new BufferedReader(new StringReader("""
'test' {
    ask null
}
    """)))

        assertThrows(PowerAssertionError.class, {
            scmDescriptor.scms()
        })
    }

    @Test
    void invalid_ask_2() {
        def scmDescriptor = new ScmDescriptor(new BufferedReader(new StringReader("""
'test' {
    ask {
        def test = hooks
    }
}
    """)))

        assertThrows(Exception.class, {
            scmDescriptor.scms()
        })
    }
}





