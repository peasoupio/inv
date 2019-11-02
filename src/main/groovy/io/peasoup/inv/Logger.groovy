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

    private static def logger = java.util.logging.Logger.getLogger("inv")


    static {

        def consoleHandler = new ConsoleHandler()
        consoleHandler.setFormatter(new SimpleFormatter() {
                @Override
                String format(LogRecord lr) {
                    return lr.getMessage() + "\n"
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


}
