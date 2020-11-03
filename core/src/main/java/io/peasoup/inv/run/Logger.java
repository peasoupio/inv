package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.StackTraceUtils;

import java.util.Queue;

public final class Logger {

    private static Queue<Object> captureQueue = null;
    private static Closure<Object> captureClosure = null;

    private static boolean systemEnabled = false;
    private static boolean debugEnabled = false;

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

    public static void system(Object arg) {
        String message = arg.toString();
        send(message);

        if (!systemEnabled)
            return;

        System.out.append("[SYSTEM] ").append(message).append(System.lineSeparator());
    }

    public static void debug(Object arg) {
        String message = arg.toString();
        send(message);

        if (!debugEnabled)
            return;

        System.out.append("[DEBUG] ").append(message).append(System.lineSeparator());
    }

    public static void info(Object arg) {
        String message = arg.toString();
        send(message);

        System.out.append("[INV] ").append(message).append(System.lineSeparator());
    }

    public static void warn(Object arg) {
        String message = arg.toString();
        send(message);

        System.err.append("[WARN] ").append(message).append(System.lineSeparator());
    }

    public static void fail(Object arg) {
        String message = arg.toString();
        send(message);

        System.err.append("[FAIL] ").append(message).append(System.lineSeparator());
    }

    public static void error(Throwable throwable) {
        StackTraceUtils.sanitize(throwable).printStackTrace();
    }

    public static void error(final String invName, final Throwable throwable) {
        send("inv: " + invName + ", message:" + throwable.getMessage());

        System.err.append("[ERROR] inv: ").append(invName).append(System.lineSeparator());
        StackTraceUtils.sanitize(throwable).printStackTrace();
    }

    public static void trace(String s) {
        send(s);

        System.out.print(s);
    }

    @SuppressWarnings("unchecked")
    public static <T> T capture(T value) {

        // Reset both so only one works at the time
        captureClosure = null;
        captureQueue = null;

        if (value instanceof Queue) captureQueue = (Queue<Object>) value;

        if (value instanceof Closure) captureClosure = (Closure<Object>) value;

        return value;
    }

    private static void send(String message) {
        if (StringUtils.isEmpty(message))
            return;

        if (captureQueue != null) captureQueue.add(message);
        if (captureClosure != null) captureClosure.call(message);
    }

    public static void resetCapture() {
        captureQueue = null;
        captureClosure = null;
    }


}
