package io.peasoup.inv

import groovy.transform.CompileStatic
import io.peasoup.inv.cli.*
import io.peasoup.inv.run.Logger
import io.peasoup.inv.run.RunsRoller
import io.peasoup.inv.security.CommonLoader
import org.codehaus.groovy.runtime.InvokerHelper
import org.docopt.Docopt
import org.docopt.DocoptExitException

@CompileStatic
class Main extends Script {

    String usage = """Inv.

Usage:
  inv run [-x] [-s] [-e <label>] <patterns>...
  inv scm [-x] [-s] <scmFiles>...
  inv composer [-x] [-s]
  inv init [-x] [-s] <scmFile>
  inv promote [<runIndex>] 
  inv delta <base> <other>
  inv graph (plain|dot) <base>
  
Options:
  run          Load and execute INV files.
  scm          Load and execute SCM files.
  composer     Start Composer dashboard
  init         Start Composer dashboard from an SCM file.
  promote      Promote a run.txt as the new base.
  delta        Generate delta between two run files.
  graph        Generate a graph representation.
  -x --debug   Debug out. Excellent for troubleshooting.
  -s --secure  Enable the secure mode for script files.
  -e --exclude Exclude files from loading.
  -h --help    Show this screen.
  
Parameters:
  <label>      Label not to be included in the loaded file path
  <patterns>   An Ant-compatible file pattern
               (p.e *.groovy, ./**/*.groovy, ...)
               Also, it is expandable using a space-separator
               (p.e myfile1.groovy myfile2.groovy)
  <scmFiles>   The SCM file(s) location.
               You can use a file ending with 'scm-list.txt'
               for it to list all your SCM file references.
               Each line must equal to the absolute path
               of your SCM file on the current filesystems.
  <scmFile>    The SCM file location.
  <runIndex>   The run index whose promotion will be granted.
               Runs are located inside INV_HOME/.runs/ 
               By default, it uses the latest successful run
               location.
  <base>       Base file location
  <other>      Other file location
  plain        No specific output structure
  dot          Graph Description Language (DOT) output structure
"""

    /**
     * Determines whether or not the main is embedded into another JVM process or has its own
     */
    static boolean embedded = false

    /**
     * Returns the latest run exit code
     */
    static int exitCode = 0

    private Map<String, Object> arguments

    @SuppressWarnings("GroovyAssignabilityCheck")
    Object run() {

        // Set current home from environment variable INV_HOME
        if (System.getenv('INV_HOME'))
            Home.setCurrent(new File(System.getenv('INV_HOME')))

        // Set current UTC timezone
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // Parse docopt arguments
        try {
            arguments = new Docopt(usage)
                    .withExit(false)
                    .parse(getProperty("args") as List<String>)
        } catch(DocoptExitException ex) {
            println usage
            return -1
        }

        // Find a matching command
        CliCommand command = findCommand()
        if (!command) {
            println usage
            return -1
        }

        // Make sure we setup the rolling mechanism property BEFORE any logging
        if (command.rolling())
            setupRolling(arguments["--debug"] as boolean)

        // Do system checks
        if (new SystemChecks().consistencyFails(this)) {
            RunsRoller.latest.latestHaveFailed()
            return -2
        }

        // Execute command
        int result

        try {
            result = command.call()
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

    CliCommand findCommand() {
        if (arguments["--secure"])
            CommonLoader.enableSecureMode()

        if (arguments["run"])
            return new RunCommand(
                    patterns: arguments["<patterns>"] as List<String>,
                    exclude: arguments["--exclude"] as String ?: "")

        if (arguments["scm"])
            return new ScmCommand(scmFiles: arguments["<scmFiles>"] as List<String>)

        if (arguments["delta"])
            return new DeltaCommand(
                    base: arguments["<base>"] as String,
                    other: arguments["<other>"] as String)

        if (arguments["graph"])
            return new GraphCommand(arguments: arguments)

        if (arguments["composer"])
            return new ComposerCommand()

        if (arguments["init"])
            return new InitCommand(scmFilePath: arguments["<scmFile>"] as String)

        if (arguments["promote"])
            return new PromoteCommand(runIndex: arguments["<runIndex>"] as String)

        return null
    }

    private static void setupRolling(boolean debug) throws IOException {
        // Roll a new run folder
        RunsRoller.getLatest().roll()

        // Enable file logging
        Logger.enableFileLogging(new File(RunsRoller.getLatest().folder(), "run.txt").getCanonicalPath())

        if (debug)
            Logger.enableDebug()
    }

    static void main(String[] args) {
        exitCode = InvokerHelper.runScript(Main, args) as int

        if (embedded)
            return

        if (exitCode == 0)
            return

        System.exit(exitCode)
    }
}