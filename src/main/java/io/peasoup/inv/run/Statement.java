package io.peasoup.inv.run;

interface Statement {

    // Identification
    Object getId();

    String getName();

    // When assigned to a Inv
    Inv getInv();

    void setInv(Inv value);

    // When processed
    StatementStatus getState();

    Manageable getMatch();

    interface Manageable<N extends Statement> {
        void manage(NetworkValuablePool pool, N networkValuable);
    }
}
