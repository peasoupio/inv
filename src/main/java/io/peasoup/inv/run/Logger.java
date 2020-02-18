package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.codehaus.groovy.runtime.StringGroovyMethods;

import java.io.File;
import java.io.IOException;
import java.util.Queue;

public class Logger {
    private static final Level INV = Level.forName("INV", Level.INFO.intLevel());
    private static org.apache.logging.log4j.Logger log = LogManager.getRootLogger();
    private static Queue captureQueue = null;
    private static Closure captureClosure = null;

    private Logger() {

    }

    public static void setupRolling() throws IOException {
        RunsRoller.getLatest().roll();

        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        // Make sur FILE is not already configured
        config.getRootLogger().removeAppender("FILE");
        ctx.updateLoggers();

        String logFilepath = new File(RunsRoller.getLatest().folder(), "run.txt").getCanonicalPath();

        // Define FILE appender
        FileAppender fileAppender = FileAppender.createAppender(logFilepath, "false", "false", "FILE", "true", "false", "false", "4000", config.getAppenders().get("stdout").getLayout(), null, "false", null, config);

        // Start FILE appender
        fileAppender.start();
        config.getRootLogger().addAppender(fileAppender, null, null);
        ctx.updateLoggers();
    }

    public static void fail(Object arg) {
        String message = arg.toString();
        send(message);

        log.fatal(message);
    }

    public static void warn(Object arg) {
        String message = arg.toString();
        send(message);

        log.warn(message);
    }

    public static void info(Object arg) {
        String message = arg.toString();
        send(message);

        log.log(INV, message);
    }

    public static void debug(Object arg) {
        String message = arg.toString();
        send(message);

        log.debug(message);
    }

    public static void error(Throwable ex) {
        log.error(ex.getMessage(), StackTraceUtils.sanitize(ex));
    }

    public static void error(final String invName, final Throwable ex) {
        String message = "inv: " + invName + ", message:" + ex.getMessage();
        send(message);

        log.error(message, StackTraceUtils.sanitize(ex));
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

    public static void enableDebug() {
        Configurator.setRootLevel(Level.DEBUG);
    }

    public static void disableDebug() {
        Configurator.setRootLevel(Level.INFO);
    }

    private static void send(String message) {
        if (!StringGroovyMethods.asBoolean(message)) return;


        if (captureQueue != null) captureQueue.add(message);

        if (captureClosure != null) captureClosure.call(message);
    }
}
