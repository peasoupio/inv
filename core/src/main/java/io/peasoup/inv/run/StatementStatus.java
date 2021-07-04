package io.peasoup.inv.run;

public enum StatementStatus {
    /**
     * Not processed yet
     */
    NOT_PROCESSED(-1),
    /**
     * Could not find a matching statement
     */
    FAILED(0),
    /**
     * Could not find a matching statement and is optional
     */
    CLEANED(1),
    /**
     * Processed during an halting cycle
     */
    HALTING(2), // Processed during halting
    /**
     * Found a matching statement
     */
    SUCCESSFUL(3),
    /**
     * Statement is already broadcasted
     */
    ALREADY_BROADCAST(4);

    public final int level;

    StatementStatus(int level) {
        this.level = level;
    }
}
