package io.peasoup.inv

import groovy.transform.CompileStatic
import io.peasoup.inv.cli.*
import io.peasoup.inv.loader.GroovyLoader
import io.peasoup.inv.repo.RepoInvoker
import io.peasoup.inv.run.InvInvoker
import io.peasoup.inv.run.Logger
import io.peasoup.inv.run.RunsRoller
import org.codehaus.groovy.runtime.InvokerHelper
import org.docopt.Docopt
import org.docopt.DocoptExitException

@CompileStatic
class Main extends Script {

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
            arguments = new Docopt(usage())
                    .withExit(false)
                    .parse(getProperty("args") as List<String>)
        } catch(DocoptExitException ex) {
            println usage()
            return -1
        }

        // Find a matching command
        CliCommand command = findCommand()
        if (!command) {
            println usage()
            return -1
        }

        // Make sure we setup the rolling mechanism property BEFORE any logging
        if (command.rolling())
            setupRolling()

        // Enable debug logs
        if (arguments["--debug"] as boolean)
            Logger.enableDebug()

        // Enable system logs
        if (arguments["--system"] as boolean)
            Logger.enableSystem()

        // Enable secure mode
        if (arguments["--secure"])
            GroovyLoader.enableSecureMode()
        else
            GroovyLoader.disableSecureMode()

        // Enable SystemClassLoader
        if (System.getProperty("java.system.class.loader"))
            GroovyLoader.enableSystemClassloader()
        else
            GroovyLoader.disableSystemClassloader()

        // Create caches for invokers
        InvInvoker.newCache()
        RepoInvoker.newCache()

        // Do system checks
        if (SystemInfo.consistencyFails()) {
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

        if (arguments["repo"]) {
            if (arguments["get"])
                return new RepoGetCommand(
                        repoUrl: arguments["<repoUrl>"] as String,
                        createParameters: arguments["--create-parameters"] as Boolean,
                        run: arguments["--run"] as Boolean)
            if (arguments["run"])
                return new RepoRunCommand(
                        repoFileLocation: arguments["<repoFile>"] as String,
                        list: arguments["--list"] as Boolean)
            if (arguments["create"]) {
                return new RepoCreateCommand()
            }
        }

        if (arguments["run"])
            return new RunCommand(
                    patterns: arguments["<include>"] as List<String>,
                    exclude: arguments["--exclude"] as String ?: "")

        if (arguments["test"])
            return new TestCommand(
                    patterns: arguments["<include>"] as List<String>,
                    exclude: arguments["--exclude"] as String ?: "")

        if (arguments["syntax"])
            return new SyntaxCommand(
                    patterns: arguments["<include>"] as List<String>,
                    exclude: arguments["--exclude"] as String ?: "")

        if (arguments["delta"])
            return new DeltaCommand(
                    base: arguments["<base>"] as String,
                    other: arguments["<other>"] as String)

        if (arguments["graph"])
            return new GraphCommand(arguments: arguments)

        if (arguments["composer"])
            return new ComposerCommand()

        if (arguments["init"])
            return new InitCommand(initRepoFileLocation: arguments["<repoFile>"] as String)

        if (arguments["promote"])
            return new PromoteCommand(runIndex: arguments["<runIndex>"] as String)

        return null
    }

    private static String usage() {
        return """Inv, version: ${SystemInfo.version()}.

Usage:
  inv (run|test|syntax) [-d | -x] [-s] [-e <exclude>] <include>...
  inv repo get [-d | -x] [-s] [-p] [-r] <repoUrl>
  inv repo run [-d | -x] [-s] [-l] <repoFile>
  inv repo create
  inv composer [-d | -x] [-s]
  inv init [-d | -x] [-s] <repoFile>
  inv promote [<runIndex>] 
  inv delta <base> <other>
  inv graph (dot) <base>
  
Options:
  run          Load and execute INV files.
  test         Load and execute a unit test script
  syntax       Test the syntax of an INV or REPO file.
  repo         Get, run or create a REPO folder.
  composer     Start Composer dashboard
  init         Start Composer dashboard from an REPO file.
  promote      Promote a run.txt as the new base.
  delta        Generate delta between two run files.
  graph        Generate a graph representation.
  -d --debug   Debug out. Excellent for troubleshooting.
  -x --system  Print system troubleshooting messages.
  -s --secure  Enable the secure mode for script files.
  -e --exclude Exclude files from loading.
  -p, --create-parameters
               Create a parameter file of a REPO file.
  -r --run     Run a REPO file after getting it.
  -l --list    Use a list of repo to run.
  -h --help    Show this screen.
  
Parameters:
  
  <include>    Indicates the files to include.
               It is Ant-compatible 
               (p.e *.groovy, ./**/*.groovy, ...)
               It is also expandable using a space-separator
               (p.e myfile1.groovy myfile2.groovy)
  <exclude>    Indicates the files to exclude.
               Exclusion is predominant over inclusion
               It is Ant-compatible 
               (p.e *.groovy, ./**/*.groovy, ...)
  <repoUrl>    The REPO remote file location.  
  <repoFile>   The REPO file location. 
               The file MUST be existing on the 
               current file system.
               Using --list with run, you can use a list
               of repos. 
  <runIndex>   The run index whose promotion will be granted.
               Runs are located inside INV_HOME/.runs/ 
               By default, it uses the latest successful run
               location.
  <base>       Base file location
  <other>      Other file location
  dot          Graph Description Language (DOT) output structure
"""
    }

    private static void setupRolling() throws IOException {
        // Roll a new run folder
        RunsRoller.getLatest().roll()
    }

    /**
     * Allows to execute CLI commands within the current process.
     * @param args CLI args
     */
    static void start(String[] args) {
        exitCode = InvokerHelper.runScript(Main, args) as int
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