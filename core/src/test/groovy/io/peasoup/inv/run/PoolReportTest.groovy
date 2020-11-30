package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertThrows

class PoolReportTest {

    PoolReport poolReport

    @Before
    void setup() {
        poolReport = new PoolReport([], new LinkedList<PoolReport.PoolError>(), false)
    }

    @Test
    void ctor() {
        def pool = new PoolReport()

        assertNotNull pool.digested
        assertNotNull pool.errors
    }

    @Test
    void fail() {
        assertThrows(IllegalArgumentException.class, {
            new PoolReport(null, new LinkedList<PoolReport.PoolError>(), false)
        })

        assertThrows(IllegalArgumentException.class, {
            new PoolReport([], null, false)
        })

        assertThrows(IllegalArgumentException.class, {
            poolReport.eat(null)
        })

        assertThrows(IllegalArgumentException.class, {
            poolReport.eat(new PoolReport(null, new LinkedList<PoolReport.PoolError>(), false))
        })

        assertThrows(IllegalArgumentException.class, {
            poolReport.eat(new PoolReport([], null, false))
        })

        assertThrows(IllegalArgumentException.class, {
            new PoolReport.PoolError(null, null)
        })

        assertThrows(IllegalArgumentException.class, {
            new PoolReport.PoolError(new Inv.Context().build(), null)
        })
    }
}
