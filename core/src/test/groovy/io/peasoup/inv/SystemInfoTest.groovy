package io.peasoup.inv

import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(TempHome.class)
class SystemInfoTest {

    @Test
    void consistencyFails() {
        // This test also means, by default, any environment should be working
        assertFalse SystemInfo.consistencyFails()
    }

    @Test
    void check_invhome() {
        assertTrue SystemInfo.checkInvHome(null)
        assertTrue SystemInfo.checkInvHome(new File("./does-not-exists/"))
        assertTrue SystemInfo.checkInvHome(new File("./pom.xml")) // MUST exists, so using pom.xml of inv
    }

    @Test
    void check_version() {
        assertNotNull SystemInfo.version()
    }
}
