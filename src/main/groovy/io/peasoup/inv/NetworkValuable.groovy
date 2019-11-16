package io.peasoup.inv

interface NetworkValuable {

    final static int NOT_PROCESSED = -1, // Not processed yet
                     FAILED = 0, // Negative match
                     UNBLOADTING = 1, // No match verified
                     SUCCESSFUL = 2, // Positive match
                     ALREADY_BROADCAST = 3 // Positive match

    final static Manageable BROADCAST = new BroadcastValuable.Broadcast()
    final static Manageable REQUIRE = new RequireValuable.Require()

    final static String DEFAULT_ID = "undefined"

    // Identification
    Object id
    String name

    Manageable match

    // When assigned to a Inv
    Inv inv

    // When processed
    int match_state

    interface Manageable<N extends NetworkValuable> {
        void manage(NetworkValuablePool pool, N networkValuable)
    }
}
