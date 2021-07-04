package io.peasoup.inv.run;

/**
 * Helps a statement with the hashing logics.
 * It is mostly useful to override @hashcode and @equals methods.
 */
public class StatementHasher {

    private StatementHasher() {
        // private ctor
    }

    /**
     * Gets the hashcode of a statement
     * @param statement The statement
     * @return The hashcode
     */
    public static int hashcode(StatementIdentifier statement) {
        if (statement == null)
            throw new IllegalArgumentException("statement");

        int hash = 7;
        hash = 31 * hash + statement.getName().hashCode();
        hash = 31 * hash + (statement.getId() == null ? 0 : statement.getId().hashCode());
        return hash;
    }

    /**
     * Determines if two statement are equal.
     * @param it The statement being compared
     * @param other The statement to compare
     * @return True if equal, otherwise false
     */
    public static boolean equals(StatementIdentifier it, Object other) {
        if (it == null)
            return false;

        if (!(other instanceof StatementIdentifier))
            return false;

        StatementIdentifier otherStatement = (StatementIdentifier)other;

        if (!it.getName().equals(otherStatement.getName()))
            return false;

        return it.getId().equals(otherStatement.getId());
    }
}
