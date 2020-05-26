package io.peasoup.inv


import org.junit.Test

class SystemInfoTest {

    @Test
    void consistncyFails() {
        // This test also means, by default, any environment should be working
        assert !SystemInfo.consistencyFails()
    }

    @Test
    void check_invhome() {
        assert SystemInfo.checkInvHome(null)
        assert SystemInfo.checkInvHome(new File("./does-not-exists/"))
        assert SystemInfo.checkInvHome(new File("./pom.xml")) // MUST exists, so using pom.xml of inv
    }

    @Test
    void check_version() {
        assert SystemInfo.version()
    }
}
