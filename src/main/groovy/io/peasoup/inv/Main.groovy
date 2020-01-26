package io.peasoup.inv

import groovy.transform.CompileStatic
import io.peasoup.inv.cli.*
import io.peasoup.inv.run.Logger
import io.peasoup.inv.run.RunsRoller
import org.codehaus.groovy.runtime.InvokerHelper
import org.docopt.Docopt
import org.docopt.DocoptExitException

@CompileStatic
class Main extends Script {

    String usage = """Inv.

Usage:
  inv run [-x] [-e <label>] <patterns>...
  inv scm [-x] <scmFiles>...
  inv composer [-x]
  inv init [-x] <scmFile>
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
               for it to list all your SCM files references.
               Each line must equal to the absolute path
               of your SCM file on the current filesystems.
  <scmFile>    The SCM file location.
  <runIndex>   The run index whom promotion will be granted.
               Runs are located inside INV_HOME/.runs/ 
               By default, it uses the latest successful run
               location.
  <base>       Base file location
  <other>      Other file location
  plain        No specific output structure
  dot          Graph Description Language (DOT) output structure
"""

    static final File DEFAULT_HOME = new File("./")
    static File currentHome

    static {
        currentHome = DEFAULT_HOME
    }

    private Map<String, Object> arguments

    @SuppressWarnings("GroovyAssignabilityCheck")
    Object run() {

        if (System.getenv('INV_HOME'))
            currentHome = new File(System.getenv('INV_HOME'))

        try {
            arguments = new Docopt(usage)
                    .withExit(false)
                    .parse(getProperty("args") as List<String>)
        } catch(DocoptExitException ex) {
            println usage
            return -1
        }

        // Resolved command
        CliCommand command = proceedWithCommands()
        if (!command) {
            println usage
            return -1
        }

        // Make sure we setup the rolling mechanism property BEFORE any logging
        if (command.rolling())
            Logger.setupRolling()

        // Do system checks
        if (new SystemChecks().consistencyFails(this)) {
            RunsRoller.latest.latestHaveFailed()
            return -2
        }

        // Execute command
        int result = command.call()

        // If rolling, make sure to update success and fail symlinks
        if (command.rolling()) {
            if (result == 0)
                RunsRoller.latest.latestHaveSucceed()
            else
                RunsRoller.latest.latestHaveFailed()
        }

        return result
    }

    CliCommand proceedWithCommands() {
        if (arguments["--debug"])
            Logger.enableDebug()

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

    static void main(String[] args) {
        InvokerHelper.runScript(Main, args)
    }

}