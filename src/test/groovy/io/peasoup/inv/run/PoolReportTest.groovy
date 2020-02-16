package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertThrows

class PoolReportTest {

    PoolReport poolReport

    @Before
    void setup() {
        poolReport = new PoolReport([], new LinkedList<PoolReport.PoolException>(), false)
    }

    @Test
    void ctor() {
        def pool = new PoolReport()

        assertNotNull pool.digested
        assertNotNull pool.exceptions
    }

    @Test
    void fail() {

        assertThrows(AssertionError.class, {
            new PoolReport(null, new LinkedList<PoolReport.PoolException>(), false)
        })

        assertThrows(AssertionError.class, {
            new PoolReport([], null, false)
        })

        assertThrows(AssertionError.class, {
            poolReport.eat(null)
        })

        assertThrows(AssertionError.class, {
            poolReport.eat(new PoolReport(null, new LinkedList<PoolReport.PoolException>(), false))
        })

        assertThrows(AssertionError.class, {
            poolReport.eat(new PoolReport([], null, false))
        })
    }
}
