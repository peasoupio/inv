package io.peasoup.inv.run;

/**
 * Defines a statement identifiable structure.
 */
public interface StatementIdentifier {

    /**
     * Gets the current NAME of this statement
     *
     * @return String reference to its NAME
     */
    String getName();

    /**
     * Gets the current ID of this statement
     *
     * @return Object reference to its ID
     */
    Object getId();

    /**
     * Before adding this statement to a map or index, the ID must be resolved to get the actual value.
     */
    void applyResolvedID();

}
