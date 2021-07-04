package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

class InvTest {

    NetworkValuablePool pool
    Inv.Builder ctx
    Inv inv

    @Before
    void setup() {
        pool = new NetworkValuablePool()
        ctx = new Inv.Builder(pool)
        inv = ctx.build()
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

    @Test
    void context_defaultName() {
        def context = new Inv.Builder(pool)
        context.setDefaultName("")

        assertNull context.getDefaultName()
    }

    @Test
    void context_defaultPath() {
        def context = new Inv.Builder(pool)
        context.setDefaultPath("")

        assertEquals Inv.Builder.WORKING_DIR, context.getDefaultPath()
    }

    @Test
    void context_repo() {
        def context = new Inv.Builder(pool)
        context.setRepo("")

        assertNull context.getRepo()
    }

    @Test
    void context_baseFilename() {
        def context = new Inv.Builder(pool)
        context.setBaseFilename("")

        assertNull context.getBaseFilename()
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
        assertEquals Inv.Builder.WORKING_DIR, inv.delegate.path
    }

    @Test
    void digest_pool_not_ok() {
        assertFalse pool.isIngesting()

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
}
