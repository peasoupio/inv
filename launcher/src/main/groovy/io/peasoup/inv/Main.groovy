package io.peasoup.inv

import groovy.transform.CompileStatic
import io.peasoup.inv.cli.*
import io.peasoup.inv.loader.GroovyLoader
import io.peasoup.inv.run.RunsRoller
import org.codehaus.groovy.runtime.InvokerHelper
import org.docopt.Docopt
import org.docopt.DocoptExitException

@CompileStatic
class Main extends Script {

    private Map<String, Object> execArguments
    private Map<String, Object> commandArguments

    @SuppressWarnings("GroovyAssignabilityCheck")
    Object run() {

        // Set current home from environment variable INV_HOME
        if (System.getenv('INV_HOME'))
            Home.setCurrent(new File(System.getenv('INV_HOME')))

        // Set current UTC timezone
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // Parse docopt arguments
        def args = (getProperty("args") as List<String>)

        // Get sanitized args
        execArguments = sanitize(args)

        // Exec arguments is empty, print usage and quit
        if (!execArguments) {
            println usage()
            return -1
        }

        // Find a matching command
        CliCommand command = findCommand(execArguments["<command>"] as String)
        if (!command) {
            println usage()
            return -2
        }


        // Parse docopt arguments
        try {
            commandArguments = new Docopt(command.usage())
                    .withExit(false)
                    .parse(args)
        } catch(DocoptExitException ex) {
            println usageHeader()
            println command.usage()
            return -3
        }

        // Make sure we setup the rolling mechanism property BEFORE any logging
        if (command.rolling())
            setupRolling()

        // Enable debug logs
        if (execArguments["--debug"] as boolean)
            Logger.enableDebug()

        // Enable system logs
        if (execArguments["--system"] as boolean)
            Logger.enableSystem()

        // Enable secure mode
        if (execArguments["--secure"])
            GroovyLoader.enableSecureMode()
        else
            GroovyLoader.disableSecureMode()

        // Do system checks
        if (SystemInfo.consistencyFails()) {
            RunsRoller.latest.latestHaveFailed()
            return -2
        }

        // Execute command
        int result

        try {
            result = command.call(commandArguments)
        } catch(Exception ex) {
            Logger.error(ex)
            result = -99
        }

        // If rolling, make sure to update success and fail symlinks
        if (command.rolling()) {
            if (result == 0)
                RunsRoller.latest.latestHaveSucceed()
            else
                RunsRoller.latest.latestHaveFailed()
        }

        return result
    }

    Map sanitize(List<String> args) {
        // Sanitized args make sure docopt does not raise exceptions
        // So, we take each argument, stack it, and test the chain to see
        // if a positive result occurs, otherwise, it fails
        // Best case scenario, if the last argument is a valid subcommand,
        // the process should go on and forget about the remaining arguments.
        def sanitizedArgs = []
        for(String arg : args) {
            sanitizedArgs << arg

            try {
                return new Docopt(usage())
                        .withExit(false)
                        .parse(sanitizedArgs)
            } catch(DocoptExitException ex) {
                // hide exception
            }
        }
    }

    CliCommand findCommand(String command) {
        switch (command) {
            case "run": return new RunCommand()
            case "syntax": return new SyntaxCommand()
            case "repo-get": return new RepoGetCommand()
            case "repo-run": return new RepoRunCommand()
            case "repo-test": return new RepoTestCommand()
            case "repo-create": return new RepoCreateCommand()
            case "composer": return new ComposerCommand()
            case "init-run": return new InitRunCommand()
            case "init-create": return new InitCreateCommand()
            case "promote": return new PromoteCommand()
            case "delta": return new DeltaCommand()
            case "graph": return new GraphCommand()
            default: return null;
        }
    }

    private String usageHeader() {
        "Inv, version: ${SystemInfo.version()}."
    }

    private String usage() {
        return """${usageHeader()}

Usage:
 inv [-dxs] <command> [<args>...]

Options:
  -h --help    Show this screen.  
  -d --debug   Debug out. Excellent for troubleshooting.  
  -x --system  Print system troubleshooting messages.  
  -s --secure  Enable the secure mode for script files.  

The subcommands are:
  run          Load and execute INV files.
  syntax       Check the syntax of an INV or REPO file.
  repo-create  Create a REPO folder.
  repo-get     Get a REPO folder.
  repo-run     Run a REPO folder.
  repo-test    Test a REPO folder.
  composer     Start Composer dashboard
  init-run     Start Composer dashboard from an REPO file
  init-run     Create an empty Git init repository.
  promote      Promote a run.txt as the new base.
  delta        Generate delta between two run files.
  graph        Generate a graph representation.

Environment variables:
  INV_HOME     Defines the working directory.
               By default it uses the current folder.
"""
    }

    private void setupRolling() throws IOException {
        // Roll a new run folder
        RunsRoller.getLatest().roll()
    }

    /**
     * Returns the latest run exit code
     */
    private static int exitCode = 0

    /**
     * Allows to execute CLI commands within the current process.
     * @param args CLI args
     */
    static void start(String... args) {
        exitCode = InvokerHelper.runScript(Main, args) as int
    }

    /**
     * Gets the current exit code
     */
    static int exitCode() {
        return exitCode
    }

    /**
     * JVM entry-point. Do not call directly
     * @param args CLI args
     */
    static void main(String[] args) {
        start(args)

        // Return exit code if != 0
        if (exitCode)
            System.exit(exitCode)
    }
}