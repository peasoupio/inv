package io.peasoup.inv.utils

import org.junit.Test

class ProgressbarTest {


    @Test
    void ok() {
        Integer limit = 159
        Integer stepToGo = limit

        Progressbar bar = new Progressbar("my-bar", limit, true)

        bar.start {
            while(stepToGo > 0) {
                bar.step()

                stepToGo--

                sleep(50)
            }
        }

        assert !bar.isRunning()
        assert bar.index.get() == limit
    }
}
