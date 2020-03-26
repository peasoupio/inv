package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

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

        assert report
        assert !report.isOk()
    }

    @Test
    void startUnbloating() {
        pool.runningState = NetworkValuablePool.RUNNING
        assert pool.startUnbloating()

        pool.runningState = NetworkValuablePool.HALTING
        assert !pool.startUnbloating()
    }

    @Test
    void startHalting() {
        pool.runningState = NetworkValuablePool.UNBLOATING
        assert pool.startHalting()

        pool.runningState = NetworkValuablePool.RUNNING
        assert !pool.startHalting()
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

        assert pool.preventUnbloating(broadcastValuable)
        assert pool.runningState == NetworkValuablePool.RUNNING
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

        assert !pool.preventUnbloating(broadcastValuable)
    }

    @Test
    void preventUnbloating_not_successful() {
        pool.runningState = NetworkValuablePool.UNBLOATING
        pool.isDigesting = true

        def broadcastValuable = new BroadcastStatement()
        broadcastValuable.state = StatementStatus.FAILED

        assert !pool.preventUnbloating(broadcastValuable)
    }

    @Test
    void preventUnbloating_not_broadcastValuable() {
        pool.runningState = NetworkValuablePool.UNBLOATING
        pool.isDigesting = true

        def requireValuable = new RequireStatement()
        requireValuable.state = StatementStatus.SUCCESSFUL

        assert !pool.preventUnbloating(requireValuable)
    }

    @Test
    void sort() {
        def invs = [
            new Inv(pool).with {
                name = "3"

                tail = false
                pop = false

                digestionSummary.unbloats = 0

                return delegate
            },

            new Inv(pool).with {
                name = "2"

                tail = false
                pop = false

                digestionSummary.unbloats = 0

                return delegate
            },
            new Inv(pool).with {
                name = "0"

                tail = false
                pop = true

                digestionSummary.unbloats = 0

                return delegate
            },

            new Inv(pool).with {
                name = "4"

                tail = true
                pop = false

                digestionSummary.unbloats = 10

                return delegate
            },
            new Inv(pool).with {
                name = "5"

                tail = true
                pop = false

                digestionSummary.unbloats = 999

                return delegate
            },
            new Inv(pool).with {
                name = "1"

                tail = false
                pop = true

                digestionSummary.unbloats = 999

                return delegate
            }
        ]

        pool.remainingInvs.clear()
        pool.remainingInvs.addAll(invs)

        def sortedInvs = pool.sortRemainingInvs()

        assert sortedInvs[0].name == "0"
        assert sortedInvs[1].name == "1"
        assert sortedInvs[2].name == "2"
        assert sortedInvs[3].name == "3"
        assert sortedInvs[4].name == "4"
        assert sortedInvs[5].name == "5"
    }
}