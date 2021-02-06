package io.peasoup.inv.run

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThrows

class BroadcastResponseTest {

    @Test
    void ok() {
        def resolvedBy = "resolvedBy"
        def responseObj = new Object()

        def response = new BroadcastResponse(resolvedBy, responseObj)

        assertEquals resolvedBy, response.resolvedBy
        assertEquals responseObj, response.response
    }

    @Test
    void ok_toString() {
        def resolvedBy = "resolvedBy"
        def responseObj = [my: "value"]

        def response = new BroadcastResponse(resolvedBy, responseObj)

        assertEquals "[my:value]", response.toString()
    }

    @Test
    void not_ok() {
        assertThrows(IllegalArgumentException.class, {
            new BroadcastResponse("", null)
        })
    }

}
