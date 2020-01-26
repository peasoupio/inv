package io.peasoup.inv

import groovy.transform.CompileStatic
import io.peasoup.inv.cli.*
import org.codehaus.groovy.runtime.InvokerHelper
import org.docopt.Docopt
import org.docopt.DocoptExitException

@CompileStatic
class Main extends Script {


    String usage = """Inv.

Usage:
  inv run [-x] [-e <label>] <pattern>...
  inv scm [-x] <scmFiles>...
  inv composer [-x]
  inv init [-x] <scmFile>
  inv delta <base> <other>
  inv graph (plain|dot) <base>
  
Options:
  run          Load and execute INV files.
  scm          Load and execute SCM files.
  composer     Start Composer dashboard
  init         Start Composer dashboard from an SCM file.
  delta        Generate delta between two run files.
  graph        Generate a graph representation.
  -x --debug   Debug out. Excellent for troubleshooting.
  -e --exclude Exclude files from loading.
  -h --help    Show this screen.
  
Parameters:
  <label>      Label not to be included in the loaded file path
  <pattern>    An Ant-compatible file pattern
               (p.e *.groovy, ./**/*.groovy, ...)
               Also, it is expandable using a space-separator
               (p.e myfile1.groovy myfile2.groovy)
  <scmFiles>   The SCM file(s) location.
               You can use a file ending with 'scm-list.txt'
               for it to list all your SCM files references.
               Each line must equal to the absolute path
               of your SCM file on the current filesystems.
  <scmFile>    The SCM file location.
  <base>       Base file location
  <other>      Other file location
  plain        No specific output structure
  dot          Graph Description Language (DOT) output structure
"""

    static final File DEFAULT_HOME = new File("./")
    static File currentHome

    @SuppressWarnings("GroovyAssignabilityCheck")
    Object run() {
        Map<String, Object> arguments

        currentHome = DEFAULT_HOME
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

        if (arguments["--debug"])
            Logger.enableDebug()

        if (arguments["run"])
            return RunCommand.call(arguments["<pattern>"] as List<String>, arguments["--exclude"] as String ?: "")

        if (arguments["scm"])
            return ScmCommand.call(arguments["<scmFiles>"] as List<String>)

        if (arguments["delta"])
            return DeltaCommand.call(arguments["<base>"] as String, arguments["<other>"] as String)

        if (arguments["graph"])
            return GraphCommand.call(arguments)

        if (arguments["composer"])
            return ComposerCommand.call()

        if (arguments["init"])
            return InitCommand.call(arguments["<scmFile>"] as String)

        println usage

        return 0
    }


    static void main(String[] args) {
        InvokerHelper.runScript(Main, args)
    }

}