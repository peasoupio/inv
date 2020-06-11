package io.peasoup.inv.run


import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class InvTest {

    NetworkValuablePool pool
    Inv.Context ctx
    Inv inv

    @Before
    void setup() {
        pool = new NetworkValuablePool()
        ctx = new Inv.Context(pool)
        inv = ctx.build()
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

        assertThrows(IllegalStateException.class) {
            inv.dumpDelegate()
        }

        assert pool.totalInvs.isEmpty()
    }

    @Test
    void dumpDelegate_without_path() {
        assert inv.delegate.path == Inv.Context.WORKING_DIR
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
            new Inv.Digestion().checkStatementResult(null, null)
        })

        assertThrows(IllegalArgumentException.class, {
            new Inv.Digestion().checkStatementResult(new NetworkValuablePool(), null)
        })

        assertThrows(IllegalArgumentException.class, {
            new Inv.Digestion().checkStatementResult(null, new RequireStatement())
        })
    }


    @Test
    void equals() {

        assert inv.name == null
        assert !inv.equals(ctx.build())

        inv.name = "my-inv"

        assert !inv.equals(null)
        assert !inv.equals("inv")
        assert !inv.equals(ctx.build())

        def other = ctx.build()
        other.name = "my-inv"

        assert inv.equals(other)
    }
}