package io.peasoup.inv.testing;

import io.peasoup.inv.Logger;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.PrintWriter;
import java.io.StringWriter;

public class InvTextListener extends RunListener {

    @Override
    public void testRunFinished(Result result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Finished. Result:").append(System.lineSeparator())
          .append(". Failures: ").append(result.getFailureCount()).append(System.lineSeparator())
          .append(". Ignored: ").append(result.getIgnoreCount()).append(System.lineSeparator())
          .append(". Tests run: ").append(result.getRunCount()).append(System.lineSeparator())
          .append(". Time: ").append(result.getRunTime()).append("ms").append(System.lineSeparator());


        if (result.getFailureCount() > 0) {
            sb.append(System.lineSeparator())
              .append("Failure(s): ").append(System.lineSeparator());

            for(Failure failure : result.getFailures()) {
                sb.append(failure.getDescription()).append(": ").append(System.lineSeparator());
                if (failure.getException() != null) {
                    StringWriter sw = new StringWriter();
                    StackTraceUtils.sanitize(failure.getException()).printStackTrace(new PrintWriter(sw));
                    sb.append(sw.toString());
                }
            }

        }

        Logger.trace(sb.toString());
    }

    @Override
    public void testStarted(Description description) {
        StringBuilder sb = new StringBuilder();
        sb.append("Test  : ").append(description.getDisplayName()).append(System.lineSeparator())
                .append("Run: ").append(System.lineSeparator());

        Logger.trace(sb.toString());
    }


}
