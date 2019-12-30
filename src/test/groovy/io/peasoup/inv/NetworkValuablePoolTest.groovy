package io.peasoup.inv

import org.junit.Before
import org.junit.Test

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
}