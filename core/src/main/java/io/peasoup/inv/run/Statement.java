package io.peasoup.inv.run;

public interface Statement {

    /**
     * Gets the current ID of this statement
     * @return Object reference to its ID
     */
    Object getId();

    /**
     * Gets the current NAME of this statement
     * @return String reference to its NAME
     */
    String getName();

    /**
     * Gets the markdown documentation text.
     * @return String reference to its markdown
     */
    String getMarkdown();

    /**
     * Gets the INV, owner of this statement.
     * @return Inv object reference
     */
    Inv getInv();

    /**
     * Sets the INV who owns this statement
     */
    void setInv(Inv value);

    /**
     * Gets the current statement status
     * @return The statement status reference.
     */
    StatementStatus getState();

    /**
     * Gets the manageable match mechanism.
     * @return The manageable object reference.
     */
    Manageable<Statement> getMatch();

    /**
     * Gets the label text. Per example : [BROADCAST] [Something] [with:'an-id']
     * @return String reference to this label.
     */
    String getLabel();

    /**
     * An interface that indicates who a statement should be manage within a running pool.
     * @param <N> The statement (sub-)type to manage.
     */
    interface Manageable<N extends Statement> {
        void manage(NetworkValuablePool pool, N statement);
    }
}
