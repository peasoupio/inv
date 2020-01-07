package io.peasoup.inv

import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
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

        assertThrows(PowerAssertionError.class, {
            pool.checkAvailability("")
        })
    }

    @Test
    void preventUnbloating() {
        pool.runningState = NetworkValuablePool.UNBLOATING
        pool.isDigesting = true

        def broadcastValuable = new BroadcastValuable()
        broadcastValuable.state = BroadcastValuable.SUCCESSFUL

        assert pool.preventUnbloating(broadcastValuable)
        assert pool.runningState == NetworkValuablePool.RUNNING
    }

    @Test
    void preventUnbloating_not_ok() {

        assertThrows(PowerAssertionError.class, {
            pool.preventUnbloating(null)
        })

        assertThrows(PowerAssertionError.class, {
            pool.isDigesting = false
            pool.preventUnbloating(new BroadcastValuable())
        })
    }

    @Test
    void preventUnbloating_not_unbloating() {
        pool.runningState = NetworkValuablePool.RUNNING
        pool.isDigesting = true

        def broadcastValuable = new BroadcastValuable()
        broadcastValuable.state = BroadcastValuable.SUCCESSFUL

        assert !pool.preventUnbloating(broadcastValuable)
    }

    @Test
    void preventUnbloating_not_successful() {
        pool.runningState = NetworkValuablePool.UNBLOATING
        pool.isDigesting = true

        def broadcastValuable = new BroadcastValuable()
        broadcastValuable.state = BroadcastValuable.FAILED

        assert !pool.preventUnbloating(broadcastValuable)
    }

    @Test
    void preventUnbloating_not_broadcastValuable() {
        pool.runningState = NetworkValuablePool.UNBLOATING
        pool.isDigesting = true

        def requireValuable = new RequireValuable()
        requireValuable.state = BroadcastValuable.SUCCESSFUL

        assert !pool.preventUnbloating(requireValuable)
    }
}