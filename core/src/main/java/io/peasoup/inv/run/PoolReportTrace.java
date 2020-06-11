package io.peasoup.inv.run;

import java.util.List;

public class PoolReportTrace {

    private final NetworkValuablePool pool;
    private final PoolReport report;

    public PoolReportTrace(NetworkValuablePool pool, PoolReport report) {
        if (pool == null) {
            throw new IllegalArgumentException("Pool is required");
        }

        if (report == null) {
            throw new IllegalArgumentException("Report is required");
        }

        this.pool = pool;
        this.report = report;
    }

    public void printPoolTrace() {

        Logger.info("Completed INV(s): " + (pool.getTotalInvs().size() - pool.getRemainingInvs().size()));
        Logger.info("Incompleted INV(s): " + pool.getRemainingInvs().size());

        // Print uncompleted INVs
        if (!pool.getRemainingInvs().isEmpty()) {
            StringBuilder output = printUncompletedInvs(pool);

            if (output.length() > 0)
                Logger.warn(output.toString());
        }

        // Print pool errors caught
        if (!report.getErrors().isEmpty()) {
            printPoolErrors();
        }

    }

    private StringBuilder printUncompletedInvs(final NetworkValuablePool pool) {
        StringBuilder output = new StringBuilder();
        PoolStateTree tree = new PoolStateTree(pool);

        output.append("Incompleted INV(s): ").append(System.lineSeparator());

        for (Inv remaining : tree.sortRemainingByRequireWeight()) {
            boolean noMoreStatement = remaining.getRemainingStatements().isEmpty() &&
                    remaining.getWhens().isEmpty();

            if (noMoreStatement) {
                output.append("- ").append(remaining).append(" has no statement and when criteria's left. Look below for exception(s).").append(System.lineSeparator());
                continue;
            }

            output.append("- ").append(remaining).append(" has ").append(remaining.getRemainingStatements().size()).append(" statement(s) and has ").append(remaining.getWhens().size()).append(" when criteria(s) left:").append(System.lineSeparator());

            // Print when's event
            printRemainingWhensEvent(output, pool, remaining);

            // Print requires statements
            printRemainingRequires(output, tree, remaining);

            // Print broadcast statements
            printRemainingBroadcasts(output, tree, remaining);
        }

        return output;
    }

    private void printRemainingWhensEvent(StringBuilder output, NetworkValuablePool pool, Inv remaining) {
        if (remaining.getWhens().isEmpty())
            return;

        output.append("\t").append(remaining.getWhens().size()).append(" when's criteria(s):").append(System.lineSeparator());

        for(WhenData remainingWhen : remaining.getWhens()) {
            String stateMessage = whenStateToMessage(remainingWhen.getProcessor().qualify(pool, remaining));
            output.append("\t\t[").append(stateMessage).append("] ").append(remainingWhen).append(System.lineSeparator());
        }
    }

    private void printRemainingRequires(StringBuilder output, PoolStateTree tree, Inv remaining) {
        final List<PoolStateTree.RemainingRequire> requirements = tree.sortRemainingRequireStatementByWeight(remaining);

        if (requirements.isEmpty())
            return;

        output.append("\t").append(requirements.size()).append(" requirement(s):").append(System.lineSeparator());

        for (PoolStateTree.RemainingRequire remainingRequire : requirements) {

            if (remainingRequire.isWouldMatch())
                output.append("\t\t[WOULD MATCH] ").append(remainingRequire.getStatement()).append(System.lineSeparator());
            else if (remainingRequire.isCouldMatch())
                output.append("\t\t[COULD MATCH] ").append(remainingRequire.getStatement()).append(System.lineSeparator());
            else if (Boolean.TRUE.equals(remainingRequire.getStatement().getUnbloatable()))
                output.append("\t\t[UNBLOATABLE] ").append(remainingRequire.getStatement()).append(System.lineSeparator());
            else
                output.append("\t\t[NOT MATCHED] ").append(remainingRequire.getStatement()).append(System.lineSeparator());
        }
    }

    private void printRemainingBroadcasts(StringBuilder output, PoolStateTree tree, Inv remaining) {
        final List<PoolStateTree.RemainingBroadcast> broadcasts = tree.getRemainingBroadcastStatements(remaining);
        if (broadcasts.isEmpty())
            return;

        output.append("\t").append(broadcasts.size()).append(" broadcast(s):").append(System.lineSeparator());
        for (PoolStateTree.RemainingBroadcast remainingBroadcast : broadcasts) {
            output.append("\t\t[REQUIRED BY ").append(remainingBroadcast.getRequireBy()).append("] ").append(remainingBroadcast.getStatement()).append(System.lineSeparator());
        }
    }

    private void printPoolErrors() {
        Logger.warn("Caught exception(s): " + report.getErrors().size());

        for (PoolReport.PoolError ex : report.getErrors()) {
            Logger.error(ex.getInv().getName(), ex.getThrowable());
        }
    }

    private String whenStateToMessage(int state) {
        switch(state) {
            case -1: return "HAS REMAININGS";
            case -2: return "NO VALID MATCH";
            case 0: return  "NO VALUE MATCH";
            case 1: return  "CREATED MATCH";
            case 2: return  "COMPLETED MATCH";
            default: return "";
        }
    }
}
