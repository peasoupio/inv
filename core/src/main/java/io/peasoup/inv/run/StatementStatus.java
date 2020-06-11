package io.peasoup.inv.run;

public enum StatementStatus {
        NOT_PROCESSED(-1), // Not processed yet
        FAILED(0), // Negative match
        UNBLOADTING(1), // No match verified
        HALTING(2), // Processed during halting
        SUCCESSFUL(3), // Positive match
        ALREADY_BROADCAST(4); // Positive match

        public final int level;

    StatementStatus(int level) {
            this.level = level;
        }
}
