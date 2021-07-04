package io.peasoup.inv.run;

public interface Statement extends StatementIdentifier {

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
     * Gets the processor associated with this statement.
     * @return The manageable object reference.
     */
    Processor<Statement> getProcessor();

    /**
     * Gets the label text. Per example : [BROADCAST] [Something] [with:'an-id']
     * @return String reference to this label.
     */
    String getLabel();

    /**
     * An interface that indicates who a statement should be process within a running pool.
     * @param <N> The statement (sub-)type to process.
     */
    interface Processor<N extends Statement> {
        void process(NetworkValuablePool pool, N statement);
    }
}
