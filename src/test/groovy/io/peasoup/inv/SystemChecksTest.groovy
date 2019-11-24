package io.peasoup.inv

import org.junit.Before
import org.junit.Test

class SystemChecksTest {

    SystemChecks checks

    @Before
    void setup() {
        checks = new SystemChecks()
    }

    @Test
    void main_consistency_fails() {

        assert checks.checkInvHome(null)
        assert checks.checkInvHome(new File("./does-not-exists/"))
        assert checks.checkInvHome(new File("./pom.xml")) // MUST exists, so using pom.xml of inv

        // IMPORTANT Read permissions are tricky to test since owner SEEMS to always have read permissions
    }
}
