package io.peasoup.inv


import groovy.cli.picocli.CliBuilder
import io.peasoup.inv.graph.DeltaGraph
import io.peasoup.inv.graph.DotGraph
import io.peasoup.inv.graph.PlainGraph
import io.peasoup.inv.scm.ScmDescriptor
import io.peasoup.inv.scm.ScmReader
import org.codehaus.groovy.runtime.InvokerHelper

class Main extends Script {

    File invHome = new File(System.getenv('INV_HOME') ?: "./")

/*
INV - Generated a INV sequence and manage past generations
Generate a new sequence:
usage: inv [options] <file>|<pattern>...
Options:
 <pattern>   Execute an Ant-compatible file pattern
             (p.e *.groovy, ...)

    Pattern is expandable using a space-separator
    (p.e myfile1.groovy myfile2.groovy)

    -e,--exclude <label>   Exclude files if containing the label
    -s,--from-scm <file>   Process the SCM file to extract or update sources
    -x,--debug             Enable debug logs
    Manage or view an old sequence:
    usage: inv [options]
    -d,--delta <previousFile>   Generate a delta from a recent execution in
    STDIN compared to a previous execution
    -g,--graph <type>           Print the graph from STDIN of a previous
    execution
    -h,--html                   Output generates an HTML file
*/

    @SuppressWarnings("GroovyAssignabilityCheck")
    Object run() {

        def commandsCli = new CliBuilder(usage:'''inv [options] <file>|<pattern>...
Options: 
 <pattern>   Execute an Ant-compatible file pattern
             (p.e *.groovy, ...)
             
             Pattern is expandable using a space-separator
             (p.e myfile1.groovy myfile2.groovy)                            
''')



        commandsCli.s(
                longOpt:'from-scm',
                convert: {
                    new File(it)
                },
                argName:'file',
                'Process the SCM file to extract or update sources')

        commandsCli.e(
                longOpt:'exclude',
                args:1,
                argName:'label',
                defaultValue: '',
                optionalArg: true,
                'Exclude files if containing the label')

        commandsCli.x(
                longOpt:'debug',
                'Enable debug logs')

        def utilsCli = new CliBuilder(usage: '''inv [options]''')

        utilsCli.g(
            type: String,
            longOpt:'graph',
            args:1,
            argName:'type',
            defaultValue: 'plain',
            optionalArg: true,
            'Print the graph from STDIN of a previous execution')

        utilsCli.d(
                longOpt:'delta',
                convert: {
                    new File(it)
                },
                argName:'previousFile',
                'Generate a delta from a recent execution in STDIN compared to a previous execution')

        utilsCli.h(
                longOpt:'html',
                'Output generates an HTML file')

        if (args.length == 0 || new SystemChecks().consistencyFails(this)) {
            println "INV - Generated a INV sequence and manage past generations"
            println "Generate a new sequence:"
            commandsCli.usage()
            println "Manage or view an old sequence:"
            utilsCli.usage()
            return -1
        }

        def commandsOptions = commandsCli.parse(args)

        if (commandsOptions.hasOption("x"))
            Logger.DebugModeEnabled = true

        def utilsOptions = utilsCli.parse(args)
        boolean hasHtml = utilsOptions.hasOption("h")

        // Handling graph option
        if (utilsOptions.hasOption("g")) {

            def graphType = utilsOptions.g

            if (graphType instanceof Boolean)
                graphType = "plain"

            return buildGraph(graphType, utilsOptions.arguments())
        }

        // Handling delta option
        if (utilsOptions.hasOption("d"))
            return delta(hasHtml: hasHtml, utilsOptions.d)

        // Handling SCM option
        if (commandsOptions.hasOption("s"))
            return launchFromSCM(commandsOptions.s, commandsOptions.arguments())

        // Otherwise, use default option : read inv files
        return executeScript(commandsOptions.arguments(), commandsOptions.e ?: "")
    }

    int executeScript(List<String> args, String exclude) {
        def inv = new InvHandler()

        args.each {
            def lookupPattern = it

            def lookupFile = new File(lookupPattern)

            if (!lookupFile.isDirectory() && lookupFile.exists())
                InvInvoker.invoke(inv, lookupFile)
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
                    InvInvoker.invoke(inv, it)
                }
            }
        }

        inv()

        return 0
    }

    int launchFromSCM(File arg1, List<String> args) {

        def invFiles = new ScmReader(arg1).execute()

        def inv = new InvHandler()

        invFiles.each { String name, ScmDescriptor.MainDescriptor repository ->

            // Manage entry points for SCM
            repository.entry.split().each {

                def scriptFile = new File(it)
                def path = repository.path

                if (!scriptFile.exists()) {
                    scriptFile = new File(path, it)
                    path = scriptFile.parentFile
                }

                if (!scriptFile.exists()) {
                    Logger.warn "${scriptFile.canonicalPath} does not exist. Won't run."
                    return
                }

                Logger.info("file: ${scriptFile.canonicalPath}")
                InvInvoker.invoke(inv, path.canonicalPath, scriptFile, name)
            }
        }

        Logger.info("[SCM] done")

        inv()

        return 0
    }

    int buildGraph(String arg1, List<String> args) {

        switch (arg1.toLowerCase()) {
            case "plain" :
                print(new PlainGraph(System.in.newReader()).echo())
                return 0
            case "dot":
                print(new DotGraph(System.in.newReader()).echo())
                return 0
        }
    }

    int delta(Map args, File arg1) {

        def delta = new DeltaGraph(arg1.newReader(), System.in.newReader())

        if (args.hasHtml)
            print(delta.html(arg1.name))
        else
            print(delta.echo())

        return 0
    }

    static void main(String[] args) {
        InvokerHelper.runScript(Main, args)
    }

}