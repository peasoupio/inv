package io.peasoup.inv.run

import groovy.transform.CompileStatic
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.FileAppender
import org.apache.logging.log4j.core.config.Configuration
import org.apache.logging.log4j.core.config.Configurator
import org.codehaus.groovy.runtime.StackTraceUtils

@CompileStatic
class Logger {

    private static org.apache.logging.log4j.Logger log = LogManager.getRootLogger()
    private static final Level INV = Level.forName("INV", Level.INFO.intLevel())

    private static volatile List captureList = null
    private static volatile Closure captureClosure = null

    static {
        disableDebug()

        // Make sure latest is ready BEFORE logging anything
        //assert RunsRoller.latest.folder().exists()
    }

    static void setupRolling() {
        RunsRoller.latest.roll()

        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration()

        // Make sur FILE is not already configured
        config.getRootLogger().removeAppender("FILE")
        ctx.updateLoggers()

        // Define FILE appender
        FileAppender fileAppender = FileAppender.createAppender(
                new File(RunsRoller.latest.folder(), "run.txt").canonicalPath,
                "false",
                "false",
                "FILE",
                "true",
                "false",
                "false",
                "4000",
                config.appenders["stdout"].layout,
                null,
                "false",
                null,
                config
        )
        
        // Start FILE appender
        fileAppender.start()
        config.rootLogger.addAppender(fileAppender, null, null)
        ctx.updateLoggers()
    }


    static void fail(Object arg) {
        String message = arg.toString()
        send(message)

        log.fatal message
    }

    static void warn(Object arg) {
        String message = arg.toString()
        send(message)

        log.warn message
    }

    static void info(Object arg) {
        String message = arg.toString()
        send(message)

        log.log INV, message
    }

    static void debug(Object arg) {
        String message = arg.toString()
        send(message)

        log.debug message
    }

    static void error(Exception ex) {
        log.error ex.getMessage(), ex
    }

    static void error(String invName, Exception ex) {
        String message = "inv: ${invName}, message:${ex.getMessage()}"
        send(message)

        log.error message, StackTraceUtils.sanitize(ex)
    }

    static Object capture(Object value) {

        // Reset both so only one works at the time
        captureClosure = null
        captureList = null

        if (value instanceof List)
            captureList = value

        if (value instanceof Closure)
            captureClosure = value

        return value
    }

    static void resetCapture() {
        captureList = null
        captureClosure = null
    }

    static void enableDebug() {
        Configurator.setRootLevel(Level.DEBUG)
    }

    static void disableDebug() {
        Configurator.setRootLevel(Level.INFO)
    }

    private static void send(String message) {
        if (!message)
            return

        if (captureList != null)
            captureList << message

        if (captureClosure)
            captureClosure.call(message)
    }
}
