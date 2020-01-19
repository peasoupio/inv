package io.peasoup.inv

import io.peasoup.inv.graph.DeltaGraph
import io.peasoup.inv.graph.RunGraph
import io.peasoup.inv.scm.ScmExecutor
import io.peasoup.inv.scm.ScmExecutor.SCMReport
import io.peasoup.inv.web.Routing
import org.codehaus.groovy.runtime.InvokerHelper
import org.docopt.Docopt
import org.docopt.DocoptExitException

class Main extends Script {


    String usage = """Inv.

Usage:
  inv load [-x] [-e <label>] <pattern>...
  inv scm [-x] <scmFiles>...
  inv delta <base> <other>
  inv graph (plain|dot) <base>
  inv web [-x]
  
Options:
  load         Load and execute INV files.
  scm          Load and execute SCM files.
  delta        Generate delta between two run files.
  graph        Generate a graph representation.
  web          Start the web interface.
  -x --debug   Debug out. Excellent for troubleshooting.
  -e --exclude Exclude files from loading.
  -h --help    Show this screen.
  
Parameters:
  <label>      Label not to be included in the loaded file path
  <pattern>    An Ant-compatible file pattern
               (p.e *.groovy, ./**/*.groovy, ...)
               Also, it is expandable using a space-separator
               (p.e myfile1.groovy myfile2.groovy)
  <scmFiles>   The SCM file location
  <base>       Base file location
  <other>      Other file location
  plain        No specific output structure
  dot          Graph Description Language (DOT) output structure 
"""

    static final File invHome = new File(System.getenv('INV_HOME') ?: "./")

    @SuppressWarnings("GroovyAssignabilityCheck")
    Object run() {

        Map<String, Object> arguments

        try {
            arguments = new Docopt(usage)
                    .withExit(false)
                    .parse(args)
        } catch(DocoptExitException ex) {
            println usage
            return -1
        }

        if (arguments["--debug"])
            Logger.enableDebug()

        if (arguments["load"])
            return executeScript(arguments["<pattern>"], arguments["--exclude"] ?: "")

        if (arguments["scm"])
            return launchFromSCM(arguments["<scmFiles>"])

        if (arguments["delta"])
            return delta(arguments["<base>"], arguments["<other>"])

        if (arguments["graph"])
            return graph(arguments)

        if (arguments["web"])
            return launchWeb()
    }

    int graph(Map arguments) {

        def base = arguments["<base>"]
        def run = new RunGraph(new File(base).newReader())

        if (arguments["plain"])
            println run.toPlainList()

        if (arguments["dot"])
            println run.toDotGraph()

        return 0
    }

    int delta(String base, String other) {

        def delta = new DeltaGraph(
                new File(base).newReader(),
                new File(other).newReader())

        /*
        if (args.hasHtml)
            print(delta.html(arg1.name))
        else
        */

        print(delta.echo())

        return 0
    }

    int launchFromSCM(List<String> args) {

        def invExecutor = new InvExecutor()
        def scmExecutor = new ScmExecutor()

        args.each {
            scmExecutor.read(new File(it))
        }

        def invFiles = scmExecutor.execute()
        invFiles.each { SCMReport report ->

            // If something happened, do not include/try-to-include into the pool
            if (!report.isOk)
                return

            def name = report.name
            def path = report.repository.path

            // Manage entry points for SCM
            report.repository.entry.each {

                def scriptFile = new File(it)

                if (!scriptFile.exists()) {
                    scriptFile = new File(path, it)
                    path = scriptFile.parentFile
                }

                if (!scriptFile.exists()) {
                    Logger.warn "${scriptFile.canonicalPath} does not exist. Won't run."
                    return
                }

                invExecutor.read(path.canonicalPath, scriptFile, name)
            }
        }

        Logger.info("[SCM] done")

        invExecutor.execute()

        return 0
    }

    int launchWeb() {
        return new Routing(workspace: invHome.absolutePath)
                .map()
    }

    int executeScript(List<String> args, String exclude) {
        def executor = new InvExecutor()

        args.each {
            def lookupPattern = it

            def lookupFile = new File(lookupPattern)

            if (!lookupFile.isDirectory() && lookupFile.exists())
                executor.read(lookupFile)
            else {

                Logger.debug "pattern without parent: ${lookupPattern}"

                // Convert Ant pattern to regex
                def resolvedPattern = lookupPattern
                        .replace("\\", "/")
                        .replace("/", "\\/")
                        .replace(".", "\\.")
                        .replace("*", ".*")
                        .replace("?", ".*")

                Logger.debug "resolved pattern: ${resolvedPattern}"

                List<File> invFiles = []
                invHome.eachFileRecurse {

                    // Won't check directory
                    if (it.isDirectory())
                        return

                    // Exclude
                    if (exclude && it.path.contains(exclude))
                        return

                    // Make sure path is using the *nix slash for folders
                    def file = it.path.replace("\\", "/")

                    if (file ==~ /.*${resolvedPattern}.*/)
                        invFiles << it
                    else
                        Logger.debug "match failed '${file}'"
                }

                invFiles.each {
                    executor.read(it)
                }
            }
        }

        executor.execute()

        return 0
    }


    static void main(String[] args) {
        InvokerHelper.runScript(Main, args)
    }

}