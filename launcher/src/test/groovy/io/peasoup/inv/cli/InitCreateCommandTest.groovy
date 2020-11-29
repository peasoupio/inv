package io.peasoup.inv.cli

import io.peasoup.inv.Home
import io.peasoup.inv.TempHome
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThrows

@RunWith(TempHome.class)
class InitCreateCommandTest {

    @Test
    void not_ok() {
        assertThrows(IllegalArgumentException.class, {
            new InitCreateCommand().call(null)
        })

        assertEquals 1, new InitCreateCommand().call([:])

        File emptyDir =  new File(Home.getCurrent(), "afolder")
        emptyDir.mkdir()
        new File(emptyDir, "afile") << "File used for ${InitCreateCommandTest.getName()}"

        assertEquals 2, new InitCreateCommand().call(["<repoName>": emptyDir.name])
    }
}
