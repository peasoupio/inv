package io.peasoup.inv.testing;

import io.peasoup.inv.Logger;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

public class InvTextListener extends RunListener {

    private final static char OUTLINE_CHAR = '#';

    @Override
    public void testRunFinished(Result result) {
        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator())
          .append("##################").append(System.lineSeparator())
          .append("# Test completed #").append(System.lineSeparator())
          .append("##################").append(System.lineSeparator())
          .append("- Failures: ").append(result.getFailureCount()).append(System.lineSeparator())
          .append("- Ignored: ").append(result.getIgnoreCount()).append(System.lineSeparator())
          .append("- Tests run: ").append(result.getRunCount()).append(System.lineSeparator())
          .append("- Time: ").append(result.getRunTime()).append("ms").append(System.lineSeparator());


        if (result.getFailureCount() > 0) {
            sb.append(System.lineSeparator())
              .append("Failure(s): ").append(System.lineSeparator());

            for(Failure failure : result.getFailures()) {
                sb.append(failure.getDescription()).append(": ").append(System.lineSeparator());

                StringWriter sw = new StringWriter();
                StackTraceUtils.sanitize(failure.getException()).printStackTrace(new PrintWriter(sw));
                sb.append(sw.toString());
            }
        }

        Logger.trace(sb.toString());
    }

    @Override
    public void testStarted(Description description) {
        StringBuilder sb = new StringBuilder();

        char[] lines = new char[description.getDisplayName().length() + 10];
        Arrays.fill(lines, OUTLINE_CHAR);
        sb.append(lines).append(System.lineSeparator());
        sb.append(OUTLINE_CHAR).append(" Test: ").append(description.getDisplayName()).append(" " + OUTLINE_CHAR).append(System.lineSeparator());
        sb.append(lines);

        Logger.trace(sb.toString());
    }


}
