package io.peasoup.inv

import org.junit.Test

import static org.junit.Assert.*

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
        assertTrue SystemInfo.checkInvHome(new File("./pom.xml")) // MUST exists, as this file is the main pom.xml of inv
    }

    @Test
    void check_version() {
        assertNotNull SystemInfo.version()
    }
}
