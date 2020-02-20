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
    void shutdown_without_executor() {
        assert !pool.shutdown()
    }
}