package io.peasoup.inv;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.StackTraceUtils;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class Logger {

    private static final Object notifier = new Object();
    private static final String ERR_MSG_TOKEN = "%e%:";

    // Highest from randomizer is around 900. 8096 seems a bit overkill, but better be safe than sorry :)
    private static final BlockingQueue<String> writingQueue = new LinkedBlockingQueue<>(8096);

    private static Collection<String> captureList = null;

    private static boolean systemEnabled = false;
    private static boolean debugEnabled = false;

    static {
        // Overrides StackTraceUtils blacklist
        System.setProperty(
                "groovy.sanitized.stacktraces",
                "groovy.,org.codehaus.groovy.,java.,javax.,sun.,gjdk.groovy.," +
                        "groovyjarjarantlr4.,org.apache.groovy.,");

        final BufferedWriter stdOut = new BufferedWriter(new OutputStreamWriter(System.out));
        final BufferedWriter stdErr = new BufferedWriter(new OutputStreamWriter(System.err));

        final int lineSeperatorLen = System.lineSeparator().length();

        // Start writing thread
        Thread loggingThread = new Thread(() -> {
            while(true) {

                try {
                    while(!writingQueue.isEmpty()) {

                        String nextMessage = writingQueue.poll();
                        if (nextMessage == null) break;

                        if (nextMessage.startsWith(ERR_MSG_TOKEN)) {
                            stdErr.write(nextMessage + System.lineSeparator(), 4, nextMessage.length() - 4 + lineSeperatorLen);

                            stdOut.flush();
                            stdErr.flush();
                        } else {
                            stdOut.write(nextMessage + System.lineSeparator(), 0, nextMessage.length() + lineSeperatorLen);
                        }
                    }

                    stdOut.flush();

                    synchronized (notifier) {
                        notifier.wait();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        }, "logging-thread");

        loggingThread.setDaemon(true);
        loggingThread.start();
    }

    private Logger() {

    }

    /**
     * Enable debug mode
     */
    public static void enableSystem() {
        systemEnabled = true;
    }

    /**
     * Enable debug mode
     */
    public static void enableDebug() {
        debugEnabled = true;
    }

    public static Collection<String> getCapture() {
        return captureList;
    }

    public static void setCapture(Collection<String> value) {
        captureList = value;
    }

    public static void system(Object arg) {
        if (systemEnabled)
            writeStdout("[SYSTEM]", arg.toString());
        else
            capture(arg.toString());
    }

    public static void debug(Object arg) {
        if (debugEnabled)
            writeStdout("[DEBUG]", arg.toString());
        else
            capture(arg.toString());
    }

    public static void info(Object arg) {
        writeStdout("[INV]", arg.toString());
    }

    public static void info(Object arg, Object... args) {
        writeStdout("[INV]", String.format(arg.toString(), args));
    }

    public static void warn(Object arg) {
        writeStderr("[WARN]", arg.toString());
    }

    public static void fail(Object arg) {
        writeStderr("[FAIL]", arg.toString());
    }

    public static void error(final String invName, final Throwable throwable) {
        writeStderr("[ERROR]", "inv: " + invName);
        error(throwable);
    }

    public static void error(Throwable throwable) {
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        StackTraceUtils.sanitize(throwable).printStackTrace(writer);

        writeStderr("", out.toString());
    }

    public static void trace(String s) {
        writeStdout("", s);
    }

    private static void writeStdout(String type, String message) {
        try {
            if (StringUtils.isNotEmpty(type))
                writingQueue.put(type + " " + message);
            else
                writingQueue.put(message);

            synchronized (notifier) {
                notifier.notify();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        capture(message);
    }

    private static void writeStderr(String type, String message) {
        try {
            writingQueue.put( ERR_MSG_TOKEN + type + " " + message);

            synchronized (notifier) {
                notifier.notify();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        capture(message);
    }



    private static void capture(String message) {
        // In real life scenario, this method would be useless
        // By putting this validation before the rest, it should
        // always indicate to the JVM to quit this method right away
        if (captureList == null)
            return;

        if (StringUtils.isEmpty(message))
            return;

        // Do not send only newline messages. it is useless here.
        if ("\n".equals(message) || System.lineSeparator().equals(message))
            return;

        captureList.add(message);
    }
}
