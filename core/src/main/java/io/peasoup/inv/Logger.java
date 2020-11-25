package io.peasoup.inv;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.StackTraceUtils;

import java.util.Collection;

public final class Logger {

    private static Collection<String> captureList = null;

    private static boolean systemEnabled = false;
    private static boolean debugEnabled = false;

    static {
        // Overrides StackTraceUtils blacklist
        System.setProperty(
                "groovy.sanitized.stacktraces",
                "groovy.,org.codehaus.groovy.,java.,javax.,sun.,gjdk.groovy.," +
                "groovyjarjarantlr4.,org.apache.groovy.,");
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

    public static void info(Object arg, Object ...args) {
        String message = arg.toString();
        message = String.format(message, args);

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
        // Only sends when it is not a newline
        send(s);

        System.out.append(s);
    }


    public static Collection<String> getCapture() {
        return captureList;
    }

    public static void setCapture(Collection<String> value) {
        captureList = value;
    }

    private static void send(String message) {
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
