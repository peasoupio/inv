package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.tinylog.configuration.Configuration;

import java.util.Queue;

public final class Logger {
    private static Queue captureQueue = null;
    private static Closure captureClosure = null;

    private Logger() {

    }

    /**
     * Enable debug mode
     */
    public static void enableDebug() {
        Configuration.set("level", "debug");
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

    /**
     * Enables the single logging file
     * @param logFilepath the log file absolute path
     */
    public static void enableFileLogging(String logFilepath) {
        Configuration.set("writer1", "file");
        Configuration.set("writer1.file", logFilepath);
        Configuration.set("writer1.format", "{message}");
    }

    public static Object capture(Object value) {

        // Reset both so only one works at the time
        captureClosure = null;
        captureQueue = null;

        if (value instanceof Queue) captureQueue = ((Queue) (value));

        if (value instanceof Closure) captureClosure = ((Closure) (value));

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
