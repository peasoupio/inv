package io.peasoup.inv.testing;

import io.peasoup.inv.run.Logger;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class InvTextListener extends RunListener {

    public void testRunStarted(Description description) throws Exception {
    }

    public void testRunFinished(Result result) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("Finished. Result:").append(System.lineSeparator())
          .append(". Failures: ").append(result.getFailureCount()).append(System.lineSeparator())
          .append(". Ignored: ").append(result.getIgnoreCount()).append(System.lineSeparator())
          .append(". Tests run: ").append(result.getRunCount()).append(System.lineSeparator())
          .append(". Time: ").append(result.getRunTime()).append("ms").append(System.lineSeparator());

        Logger.trace(sb.toString());
    }

    public void testSuiteStarted(Description description) throws Exception {
    }

    public void testSuiteFinished(Description description) throws Exception {
    }

    @Override
    public void testStarted(Description description) {
        StringBuilder sb = new StringBuilder();
        sb.append("Test  : ").append(description.getDisplayName()).append(System.lineSeparator())
                .append("Run: ").append(System.lineSeparator());

        Logger.trace(sb.toString());
    }

    public void testFinished(Description description) throws Exception {
    }

    public void testFailure(Failure failure) throws Exception {
    }

    public void testAssumptionFailure(Failure failure) {
    }

    public void testIgnored(Description description) throws Exception {
    }


}
