package io.peasoup.inv.utils

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse

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

                sleep(10)
            }
        }

        assertFalse bar.isRunning()
        assertEquals limit, bar.index.get()
    }
}
