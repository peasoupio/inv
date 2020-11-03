package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.*

class NetworkValuablePoolTest {

    NetworkValuablePool pool

    @Before
    void setup() {
        pool = new NetworkValuablePool()
    }

    @Test
    void digest_during_halting() {
        pool.startUnbloating()
        pool.startHalting()

        def report = pool.digest()

        assertNotNull report
        assertFalse report.isOk()
    }

    @Test
    void startUnbloating() {
        pool.runningState = NetworkValuablePool.RUNNING
        assertTrue pool.startUnbloating()

        pool.runningState = NetworkValuablePool.HALTING
        assertFalse pool.startUnbloating()
    }

    @Test
    void startHalting() {
        pool.runningState = NetworkValuablePool.UNBLOATING
        assertTrue pool.startHalting()

        pool.runningState = NetworkValuablePool.RUNNING
        assertFalse pool.startHalting()
    }

    @Test
    void checkAvailability_not_ok() {

        assertThrows(IllegalArgumentException.class, {
            pool.checkAvailability(null)
        })

        assertThrows(IllegalArgumentException.class, {
            pool.checkAvailability("")
        })
    }

    @Test
    void preventUnbloating() {
        pool.runningState = NetworkValuablePool.UNBLOATING
        pool.isDigesting = true

        def broadcastValuable = new BroadcastStatement()
        broadcastValuable.state = StatementStatus.SUCCESSFUL

        assertTrue pool.preventUnbloating(broadcastValuable)
        assertEquals NetworkValuablePool.RUNNING, pool.runningState
    }

    @Test
    void preventUnbloating_not_ok() {

        assertThrows(IllegalArgumentException.class, {
            pool.preventUnbloating(null)
        })

        assertThrows(IllegalArgumentException.class, {
            pool.isDigesting = false
            pool.preventUnbloating(new BroadcastStatement())
        })
    }

    @Test
    void preventUnbloating_not_unbloating() {
        pool.runningState = NetworkValuablePool.RUNNING
        pool.isDigesting = true

        def broadcastValuable = new BroadcastStatement()
        broadcastValuable.state = StatementStatus.SUCCESSFUL

        assertFalse pool.preventUnbloating(broadcastValuable)
    }

    @Test
    void preventUnbloating_not_successful() {
        pool.runningState = NetworkValuablePool.UNBLOATING
        pool.isDigesting = true

        def broadcastValuable = new BroadcastStatement()
        broadcastValuable.state = StatementStatus.FAILED

        assertFalse pool.preventUnbloating(broadcastValuable)
    }

    @Test
    void preventUnbloating_not_broadcastValuable() {
        pool.runningState = NetworkValuablePool.UNBLOATING
        pool.isDigesting = true

        def requireValuable = new RequireStatement()
        requireValuable.state = StatementStatus.SUCCESSFUL

        assertFalse pool.preventUnbloating(requireValuable)
    }

    @Test
    void sort() {
        def ctx = new Inv.Context(pool)
        def invs = [
            ctx.build().with {
                delegate.delegate.with {
                    name "3"
                    tail false
                    pop false
                }
                dumpDelegate()
                digestionSummary.unbloats = 0

                return delegate
            },

            ctx.build().with {
                delegate.delegate.with {
                    name "2"
                    tail false
                    pop false
                }
                dumpDelegate()
                digestionSummary.unbloats = 0

                return delegate
            },
            ctx.build().with {
                delegate.delegate.with {
                    name "0"
                    tail false
                    pop true
                }
                dumpDelegate()
                digestionSummary.unbloats = 0

                return delegate
            },

            ctx.build().with {
                delegate.delegate.with {
                    name "4"
                    tail true
                    pop false
                }
                dumpDelegate()
                digestionSummary.unbloats = 10

                return delegate
            },
            ctx.build().with {
                delegate.delegate.with {
                    name "5"
                    tail true
                    pop false
                }
                dumpDelegate()
                digestionSummary.unbloats = 999

                return delegate
            },
            ctx.build().with {
                delegate.delegate.with {
                    name "1"
                    tail false
                    pop true
                }
                dumpDelegate()
                digestionSummary.unbloats = 999

                return delegate
            }
        ]

        pool.remainingInvs.clear()
        pool.remainingInvs.addAll(invs)

        def sortedInvs = pool.sort()

        assertEquals "0", sortedInvs[0].name
        assertEquals "1", sortedInvs[1].name
        assertEquals "2", sortedInvs[2].name
        assertEquals "3", sortedInvs[3].name
        assertEquals "4", sortedInvs[4].name
        assertEquals "5", sortedInvs[5].name
    }
}