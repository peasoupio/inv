package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.codehaus.groovy.runtime.StringGroovyMethods;

import java.util.Queue;

public class Logger {
    private static Queue captureQueue = null;
    private static Closure captureClosure = null;

    private Logger() {

    }

    public static void fail(Object arg) {
        String message = arg.toString();
        send(message);

        org.tinylog.Logger.error("[FAIL] " + message);
    }

    public static void warn(Object arg) {
        String message = arg.toString();
        send(message);

        org.tinylog.Logger.warn("[WARN] " + message);
    }

    public static void info(Object arg) {
        String message = arg.toString();
        send(message);

        org.tinylog.Logger.info("[INV] " + message);
    }

    public static void debug(Object arg) {
        String message = arg.toString();
        send(message);

        org.tinylog.Logger.debug("[DEBUG] " + message);
    }

    public static void error(Throwable throwable) {
        org.tinylog.Logger.error(StackTraceUtils.sanitize(throwable), "[ERROR] " + throwable.getMessage());
    }

    public static void error(final String invName, final Throwable throwable) {
        send("inv: " + invName + ", message:" + throwable.getMessage());

        org.tinylog.Logger.error(StackTraceUtils.sanitize(throwable), "[ERROR] inv: " + invName);
    }

    public static Object capture(Object value) {

        // Reset both so only one works at the time
        captureClosure = null;
        captureQueue = null;

        if (value instanceof Queue) captureQueue = ((Queue) (value));

        if (value instanceof Closure) captureClosure = ((Closure) (value));

        return value;
    }

    public static void resetCapture() {
        captureQueue = null;
        captureClosure = null;
    }

    private static void send(String message) {
        if (!StringGroovyMethods.asBoolean(message)) return;


        if (captureQueue != null) captureQueue.add(message);

        if (captureClosure != null) captureClosure.call(message);
    }
}
