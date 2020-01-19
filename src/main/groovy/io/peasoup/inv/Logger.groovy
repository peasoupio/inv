package io.peasoup.inv

import groovy.transform.CompileStatic
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.Level
import org.apache.log4j.PatternLayout
import org.apache.log4j.spi.LoggingEvent

/*
import java.util.logging.ConsoleHandler
import java.util.logging.LogRecord
import java.util.logging.SimpleFormatter

 */

@CompileStatic
class Logger {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getRootLogger()

    private static volatile List captureList = null
    private static volatile Closure captureClosure = null

    static {
        log.with {
            disableDebug()

            def console = new ConsoleAppender()
            console.setName("Main Appender")
            console.setWriter(new OutputStreamWriter(System.out))
            console.setLayout(new PatternLayout("[%p] %m%n"))
            addAppender(console)

            // Callback
            addAppender(new AppenderSkeleton() {
                protected void append(LoggingEvent loggingEvent) {
                    if (captureList != null)
                        captureList << loggingEvent.message

                    if (captureClosure)
                        captureClosure.call(loggingEvent.message)
                }

                void close() {
                }

                boolean requiresLayout() {
                    return false
                }
            })
        }
    }

    static void fail(Object arg) {
        log.fatal arg.toString()
    }

    static void warn(Object arg) {
        log.warn arg.toString()
    }

    static void info(Object arg) {
        log.log InvLogingLevel.INV, arg.toString()
    }

    static void debug(Object arg) {
        log.debug arg.toString()
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
        log.level = Level.DEBUG
    }

    static void disableDebug() {
        log.level = Level.INFO
    }

    static class InvLogingLevel extends Level {

        public static final int INV_LEVEL_INT = Level.INFO_INT + 1

        public static final Level INV = new InvLogingLevel(INV_LEVEL_INT,"INV",7)

        protected InvLogingLevel(int level, String levelStr, int syslogEquivalent) {
            super(level, levelStr, syslogEquivalent)
        }
    }
}
