package io.peasoup.inv

import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class SystemChecksTest {

    SystemChecks checks

    @Before
    void setup() {
        checks = new SystemChecks()
    }

    @Test
    void consistncyFails() {
        // This test also means, by default, any environment should be working
        assert !checks.consistencyFails(new Main())
    }

    @Test
    void check_invhome() {

        assert checks.checkInvHome(null)
        assert checks.checkInvHome(new File("./does-not-exists/"))
        assert checks.checkInvHome(new File("./pom.xml")) // MUST exists, so using pom.xml of inv

        // IMPORTANT Read permissions are tricky to test since owner SEEMS to always have read permissions

    }

    @Test
    void check_cache() {

        assert checks.checkCache(null)
        assert checks.checkCache(new File("./pom.xml"))

        def notWritable = new File("./not-writable/")
        notWritable.deleteDir()
        notWritable.mkdirs()
        notWritable.setWritable(false)

        assert checks.checkCache(new File(notWritable, "something/"))
    }

    @Test()
    void call_not_ok() {

        assertThrows(PowerAssertionError.class, {
            checks.consistencyFails(null)
        })
    }
}
