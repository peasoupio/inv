package io.peasoup.inv

import java.util.logging.ConsoleHandler
import java.util.logging.LogRecord
import java.util.logging.SimpleFormatter


class Logger {

    /**
     * Determine if debug mode is enable.
     * Debug mode shows debug logs into the stdout
     */
    public static boolean DebugModeEnabled = false

    private static List captureList = null
    private static Closure captureClosure = null
    private static def logger = java.util.logging.Logger.getLogger("inv")


    static {

        def consoleHandler = new ConsoleHandler()
        consoleHandler.setFormatter(new SimpleFormatter() {
                @Override
                String format(LogRecord lr) {

                    def message = lr.getMessage()

                    if (captureList != null)
                        captureList << message

                    if (captureClosure)
                        captureClosure.call(message)

                    return message + "\n"
                }
            })

        logger.setUseParentHandlers(false)
        logger.addHandler(consoleHandler)
    }


    static void info(Object arg) {
        logger.info "[INV] ${arg}"
    }

    static void debug(Object arg) {
        if (!DebugModeEnabled)
            return

        logger.info "[DEBUG] ${arg}"
    }

    static void warn(Object arg) {
        logger.info "[WARN] ${arg}"
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
}
