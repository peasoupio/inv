package io.peasoup.inv.io

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThrows

class FileUtilsTest {

    @Test
    void addSubordinateSlash() {
        assertEquals "test/", FileUtils.addSubordinateSlash("test")
        assertEquals "test/", FileUtils.addSubordinateSlash("test/")
    }

    @Test
    void convertUnixPath() {
        assertEquals "test/ok", FileUtils.convertUnixPath("test/ok")
        assertEquals "test/ok", FileUtils.convertUnixPath("test\\ok")
    }

    @Test
    void not_ok() {
        assertThrows(IllegalArgumentException.class, {
            FileUtils.addSubordinateSlash("")
        })

        assertThrows(IllegalArgumentException.class, {
            FileUtils.convertUnixPath("")
        })
    }
}
