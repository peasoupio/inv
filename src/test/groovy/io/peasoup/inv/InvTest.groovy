package io.peasoup.inv

import org.junit.Before
import org.junit.Test

import static junit.framework.Assert.assertEquals

class InvTest {

    Inv inv

    @Before
    void setup() {
        inv = new Inv()
    }

    @Test
    void default_name_ok() {
        String name = "my-defaultName"

        inv.dumpDelegate(name)

        assertEquals inv.name, name
    }

    @Test
    void default_name_not_ok() {
        String name = "my-defaultName"

        inv.delegate.name = "my-real-name"

        inv.dumpDelegate(name)

        assertEquals inv.name, inv.delegate.name
    }
}
