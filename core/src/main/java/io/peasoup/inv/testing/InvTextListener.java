package io.peasoup.inv.testing;

import org.junit.internal.JUnitSystem;
import org.junit.internal.TextListener;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

import java.io.PrintStream;

public class InvTextListener extends TextListener {

    private final PrintStream writer;

    public InvTextListener(JUnitSystem system) {
        this(system.out());
    }

    public InvTextListener(PrintStream writer) {
        super(writer);

        this.writer = writer;
    }

    @Override
    public void testStarted(Description description) {
        this.writer.append("Test  : ").append(description.getDisplayName()).append(System.lineSeparator()).append("Output: ").append(System.lineSeparator());
    }

    @Override
    public void testFailure(Failure failure) {
        // do nothing
    }

    @Override
    public void testIgnored(Description description) {
        // do nothing
    }


}
