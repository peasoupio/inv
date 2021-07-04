package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

class NetworkValuablePoolTest {

    NetworkValuablePool pool

    @Before
    void setup() {
        pool = new NetworkValuablePool()
    }

    @Test
    void digest_during_halting() {
        pool.startCleaning()
        pool.startHalting()

        def report = pool.digest()

        assertNotNull report
        assertFalse report.isOk()
    }

    @Test
    void startCleaning() {
        pool.runningState = NetworkValuablePool.RUNNING
        assertTrue pool.startCleaning()

        pool.runningState = NetworkValuablePool.HALTING
        assertFalse pool.startCleaning()
    }

    @Test
    void startHalting() {
        pool.runningState = NetworkValuablePool.CLEANING
        assertTrue pool.startHalting()

        pool.runningState = NetworkValuablePool.RUNNING
        assertFalse pool.startHalting()
    }

    @Test
    void preventCleaning() {
        pool.runningState = NetworkValuablePool.CLEANING
        pool.isIngesting = true

        def broadcastValuable = new BroadcastStatement()
        broadcastValuable.state = StatementStatus.SUCCESSFUL

        assertTrue pool.preventCleaning(broadcastValuable)
        assertEquals NetworkValuablePool.RUNNING, pool.runningState
    }

    @Test
    void preventCleaning_not_ok() {

        assertThrows(IllegalArgumentException.class, {
            pool.preventCleaning(null)
        })

        assertThrows(IllegalArgumentException.class, {
            pool.isIngesting = false
            pool.preventCleaning(new BroadcastStatement())
        })
    }

    @Test
    void preventCleaning_not_cleaning() {
        pool.runningState = NetworkValuablePool.RUNNING
        pool.isIngesting = true

        def broadcastValuable = new BroadcastStatement()
        broadcastValuable.state = StatementStatus.SUCCESSFUL

        assertFalse pool.preventCleaning(broadcastValuable)
    }

    @Test
    void preventCleaning_not_successful() {
        pool.runningState = NetworkValuablePool.CLEANING
        pool.isIngesting = true

        def broadcastValuable = new BroadcastStatement()
        broadcastValuable.state = StatementStatus.FAILED

        assertFalse pool.preventCleaning(broadcastValuable)
    }

    @Test
    void preventCleaning_not_broadcastValuable() {
        pool.runningState = NetworkValuablePool.CLEANING
        pool.isIngesting = true

        def requireValuable = new RequireStatement()
        requireValuable.state = StatementStatus.SUCCESSFUL

        assertFalse pool.preventCleaning(requireValuable)
    }

    @Test
    void sort() {
        def ctx = new Inv.Builder(pool)
        def invs = [
            ctx.build().with {
                delegate.delegate.with {
                    name "3"
                    tail false
                    pop false
                }
                dumpDelegate()

                return delegate
            },

            ctx.build().with {
                delegate.delegate.with {
                    name "2"
                    tail false
                    pop false
                }
                dumpDelegate()

                return delegate
            },
            ctx.build().with {
                delegate.delegate.with {
                    name "0"
                    tail false
                    pop true
                }
                dumpDelegate()

                return delegate
            },

            ctx.build().with {
                delegate.delegate.with {
                    name "4"
                    tail true
                    pop false
                }
                dumpDelegate()
                10.times { digestionSummary.useCleaned().add(null) }

                return delegate
            },
            ctx.build().with {
                delegate.delegate.with {
                    name "5"
                    tail true
                    pop false
                }
                dumpDelegate()
                999.times { digestionSummary.useCleaned().add(null) }

                return delegate
            },
            ctx.build().with {
                delegate.delegate.with {
                    name "1"
                    tail false
                    pop true
                }
                dumpDelegate()
                999.times { digestionSummary.useCleaned().add(null) }

                return delegate
            }
        ]

        pool.remainingInvs.clear()
        pool.remainingInvs.addAll(invs)

        def sortedInvs = pool.sortRemainings()

        assertEquals "0", sortedInvs[0].name
        assertEquals "1", sortedInvs[1].name
        assertEquals "3", sortedInvs[2].name
        assertEquals "2", sortedInvs[3].name
        assertEquals "4", sortedInvs[4].name
        assertEquals "5", sortedInvs[5].name
    }

    @Test
    void include_not_ok() {
        def ctx = new Inv.Builder(pool)
        def inv = ctx.build()

        assertThrows(IllegalArgumentException.class) {
            pool.add(null, false)
        }

        assertFalse pool.add(inv, false) // missing name
    }
}