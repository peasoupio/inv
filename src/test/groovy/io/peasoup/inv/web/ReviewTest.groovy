package io.peasoup.inv.web


import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class ReviewTest {

    @Test
    void ok() {
        def base =  new File(getClass().getResource('/logOutput1.txt').toURI())
        def other =  new File(getClass().getResource('/logAfterOutput1.txt').toURI())

        def review = new Review(base, other)

        assert review.deltaGraph
        assert review.toMap().lines
    }

    @Test
    void not_ok() {

        def existing = new File(getClass().getResource('/logOutput1.txt').toURI())

        assertThrows(AssertionError.class, {
            new Review(null, null)
        })

        assertThrows(AssertionError.class, {
            new Review(existing, null)
        })

        assertThrows(AssertionError.class, {
            new Review(null, existing)
        })

        assertThrows(AssertionError.class, {
            new Review(existing, new File("not-existing"))
        })

        assertThrows(AssertionError.class, {
            new Review(new File("not-existing"), existing)
        })
    }
}
