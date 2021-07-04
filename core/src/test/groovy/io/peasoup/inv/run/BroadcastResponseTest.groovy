package io.peasoup.inv.run

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThrows

class BroadcastResponseTest {

    @Test
    void ok() {
        def resolvedBy = "resolvedBy"
        def responseObj = new Object()

        NetworkValuablePool pool = new NetworkValuablePool()
        Inv inv = new Inv.Builder(pool).build()
        inv.name = resolvedBy

        def response = new BroadcastResponse(new BroadcastStatement(
                inv: inv,
                global: {
                    return responseObj
                }))

        assertEquals resolvedBy, response.resolvedBy
        assertEquals responseObj, response.globalResponse
    }

    @Test
    void ok_toString() {

        def resolvedBy = "resolvedBy"
        def responseObj = [my: "value"]

        NetworkValuablePool pool = new NetworkValuablePool()
        Inv inv = new Inv.Builder(pool).build()
        inv.name = resolvedBy

        def response = new BroadcastResponse(new BroadcastStatement(
                inv: inv,
                global: {
                    return responseObj
                }))

        assertEquals "[my:value]", response.toString()
    }

    @Test
    void not_ok() {
        assertThrows(IllegalArgumentException.class, {
            new BroadcastResponse(null)
        })
    }

}
