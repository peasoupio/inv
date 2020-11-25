package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

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
        assertTrue pool.totalInvs.isEmpty()
        assertNull inv.name

        inv.name = "my-name"
        inv.dumpDelegate()

        assertFalse pool.totalInvs.isEmpty()
        assertEquals inv, pool.totalInvs.getAt(0)
    }

    @Test
    void dumpDelegate_without_name() {
        assertTrue pool.totalInvs.isEmpty()
        assertNull inv.name

        assertThrows(IllegalStateException.class) {
            inv.dumpDelegate()
        }

        assertTrue pool.totalInvs.isEmpty()
    }

    @Test
    void dumpDelegate_without_path() {
        assertEquals Inv.Context.WORKING_DIR, inv.delegate.path
    }

    @Test
    void digest_pool_not_ok() {
        assertFalse pool.isDigesting()

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
        assertNull inv.name
        assertFalse inv.equals(ctx.build())

        inv.name = "my-inv"

        assertFalse inv.equals(null)
        assertFalse inv.equals("inv")
        assertFalse inv.equals(ctx.build())

        def other = ctx.build()
        other.name = "my-inv"

        assertTrue inv.equals(other)
    }
}
