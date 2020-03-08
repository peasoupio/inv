package io.peasoup.inv.run


import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class InvTest {

    NetworkValuablePool pool
    Inv inv

    @Before
    void setup() {
        pool = new NetworkValuablePool()
        inv = new Inv(pool)
    }

    @Test
    void dumpDelegate_ok() {
        assert pool.totalInvs.isEmpty()
        assert !inv.name

        inv.name = "my-name"
        inv.dumpDelegate()

        assert !pool.totalInvs.isEmpty()
        assert pool.totalInvs.getAt(0) == inv
    }

    @Test
    void dumpDelegate_without_name() {
        assert pool.totalInvs.isEmpty()
        assert !inv.name

        inv.dumpDelegate()

        assert pool.totalInvs.isEmpty()
    }

    @Test
    void digest_pool_not_ok() {

        assert !pool.isDigesting()

        assertThrows(IllegalArgumentException.class, {
            inv.digest()
        })
    }

    @Test
    void digestion_concat_null() {
        new Inv.Digestion().concat(null)
    }


    @Test
    void digestion_fail() {
        assertThrows(IllegalArgumentException.class, {
            new Inv.Digestion().addResults(null)
        })
    }


    @Test
    void equals() {

        assert inv.name == null
        assert !inv.equals(new Inv(pool))

        inv.name = "my-inv"

        assert !inv.equals(null)
        assert !inv.equals("inv")
        assert !inv.equals(new Inv(pool))

        def other = new Inv(pool)
        other.name = "my-inv"

        assert inv.equals(other)
    }
}
