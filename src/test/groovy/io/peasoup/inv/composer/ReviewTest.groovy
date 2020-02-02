package io.peasoup.inv.composer

import org.junit.Test

class ReviewTest {

    @Test
    void ok() {
        def base =  new File(getClass().getResource('/logOutput1.txt').toURI())
        def other =  new File(getClass().getResource('/logAfterOutput1.txt').toURI())

        def review = new Review()

        assert review.compare(base, other).lines
    }
}
