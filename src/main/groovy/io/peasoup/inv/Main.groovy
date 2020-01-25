package io.peasoup.inv

import groovy.io.FileType
import groovy.transform.CompileStatic
import io.peasoup.inv.composer.WebServer
import io.peasoup.inv.graph.DeltaGraph
import io.peasoup.inv.graph.RunGraph
import io.peasoup.inv.scm.ScmExecutor
import io.peasoup.inv.scm.ScmExecutor.SCMReport
import io.peasoup.inv.utils.Progressbar
import org.codehaus.groovy.runtime.InvokerHelper
import org.docopt.Docopt
import org.docopt.DocoptExitException

@CompileStatic
class Main extends Script {


    String usage = """Inv.

Usage:
  inv run [-x] [-e <label>] <pattern>...
  inv scm [-x] <scmFiles>...
  inv delta <base> <other>
  inv graph (plain|dot) <base>
  inv composer [-x]
  
Options:
  run         Load and execute INV files.
  scm          Load and execute SCM files.
  delta        Generate delta between two run files.
  graph        Generate a graph representation.
  composer     Start Composer dashboard
  -x --debug   Debug out. Excellent for troubleshooting.
  -e --exclude Exclude files from loading.
  -h --help    Show this screen.
  
Parameters:
  <label>      Label not to be included in the loaded file path
  <pattern>    An Ant-compatible file pattern
               (p.e *.groovy, ./**/*.groovy, ...)
               Also, it is expandable using a space-separator
               (p.e myfile1.groovy myfile2.groovy)
  <scmFiles>   The SCM file location.
               You can use a file ending with 'scm-list.txt'
               for it to list all your SCM files references.
               Each line must equal to the absolute path
               of your SCM file on the current filesystems.
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
            return executeScript(arguments["<pattern>"] as List<String>, arguments["--exclude"] as String ?: "")

        if (arguments["scm"])
            return launchFromSCM(arguments["<scmFiles>"] as List<String>)

        if (arguments["delta"])
            return delta(arguments["<base>"] as String, arguments["<other>"] as String)

        if (arguments["graph"])
            return graph(arguments)

        if (arguments["composer"])
            return launchComposer()

        println usage

        return 0
    }

    int graph(Map arguments) {

        String base = arguments["<base>"] as String
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


        if (args.size() == 1 && args[0].endsWith("scm-list.txt")) {
            def scmListPath = args[0]
            def scmListFile = new File(scmListPath)

            if (!scmListFile.exists())
                return -1

            def lines = scmListFile.readLines()

            def progress = new Progressbar("Reading SCM from '${scmListPath}'".toString(), lines.size(), false)
            progress.start {

                lines.each {
                    scmExecutor.read(new File(it))

                    progress.step()
                }
            }
        } else {
            def progress = new Progressbar("Reading SCM from args".toString(), args.size(), false)
            progress.start {

                args.each {
                    scmExecutor.read(new File(it))

                    progress.step()
                }
            }
        }

        def scmFiles = scmExecutor.execute()
        def invsFiles = scmFiles.collectMany { SCMReport report ->

            // If something happened, do not include/try-to-include into the pool
            if (!report.isOk)
                return []

            def name = report.name

            // Manage entry points for SCM
            return report.repository.entry.collect {

                def path = report.repository.path
                def scriptFile = new File(it)

                if (!scriptFile.exists()) {
                    scriptFile = new File(path, it)
                    path = scriptFile.parentFile
                }

                if (!scriptFile.exists()) {
                    Logger.warn "${scriptFile.canonicalPath} does not exist. Won't run."
                    return
                }

                return [
                    name: name,
                    path: path.canonicalPath,
                    scriptFile: scriptFile
                ]
            }
        } as List<Map>

        def progress = new Progressbar("Reading INV files from scm".toString(), invsFiles.size(), false)
        progress.start {

            invsFiles.each {
                invExecutor.read(
                        it.path as String,
                        it.scriptFile as File,
                        it.name as String)

                progress.step()
            }
        }

        Logger.info("[SCM] done")

        invExecutor.execute()

        return 0
    }

    int launchComposer() {
        return new WebServer(workspace: currentHome.absolutePath)
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
                currentHome.eachFileRecurse(FileType.FILES) {

                    // Exclude
                    if (exclude && it.path.contains(exclude))
                        return

                    // Make sure path is using the *nix slash for folders
                    def file = it.path.replace("\\", "/")

                    if (!(file ==~ /.*${resolvedPattern}.*/))
                        return

                    invFiles << it
                }

                def progress = new Progressbar("Reading INV files from args".toString(), invFiles.size(), false)
                progress.start {

                    invFiles.each {
                        executor.read(it)

                        progress.step()
                    }
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