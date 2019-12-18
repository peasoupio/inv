package io.peasoup.inv

import groovy.transform.CompileStatic

@CompileStatic
interface NetworkValuable {

    final static int NOT_PROCESSED = -1, // Not processed yet
                     FAILED = 0, // Negative match
                     UNBLOADTING = 1, // No match verified
                     HALTING = 2, // Processed during halting
                     SUCCESSFUL = 3, // Positive match
                     ALREADY_BROADCAST = 4 // Positive match


    final static String DEFAULT_ID = "undefined"

    // Identification
    Object getId()
    String getName()


    // When assigned to a Inv
    Inv getInv()
    void setInv(Inv value)

    // When processed
    int getState()

    Manageable getMatch()

    interface Manageable<N extends NetworkValuable> {
        void manage(NetworkValuablePool pool, N networkValuable)
    }
}
