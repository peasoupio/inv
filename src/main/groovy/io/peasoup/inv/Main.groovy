package io.peasoup.inv

import groovy.cli.commons.CliBuilder
import io.peasoup.inv.graph.DeltaGraph
import io.peasoup.inv.graph.DotGraph
import io.peasoup.inv.graph.PlainGraph
import io.peasoup.inv.scm.ScmReader
import org.codehaus.groovy.runtime.InvokerHelper

class Main extends Script {

    def invHome = new File(System.getenv('INV_HOME') ?: "./")

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


        if (args.length == 0 || consistencyFails()) {
            println "INV - Generated a INV sequence and manage past generations"
            println "Generate a new sequence:"
            commandsCli.usage()
            println "Manage or view an old sequence:"
            utilsCli.usage()
            return -1
        }

        def commandsOptions = commandsCli.parse(args)

        boolean hasDebug = commandsOptions.hasOption("x")
        if (hasDebug)
            Logger.DebugModeEnabled = true

        def utilsOptions = utilsCli.parse(args)
        boolean hasHtml = utilsOptions.hasOption("h")


        if (utilsOptions.hasOption("g"))
            return buildGraph(utilsOptions.g, utilsOptions.arguments())

        if (utilsOptions.hasOption("d"))
            return delta(hasHtml: hasHtml, utilsOptions.d)

        if (commandsOptions.hasOption("s"))
            return launchFromSCM(commandsOptions.s, commandsOptions.arguments())

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

        invFiles.each { String name, File script ->

            if (!script.exists()) {
                Logger.warn "${script.canonicalPath} does not exist. Won't run."
                return
            }

            Logger.info("file: ${script.canonicalPath}")
            InvInvoker.invoke(inv, script, name)
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

    /*
        Check whether or not the system fails to meet the minimal "consistency" requirements
     */
    boolean consistencyFails() {

        Logger.debug "INV_HOME: ${invHome.absolutePath}"

        if (!invHome.isDirectory()) {
            Logger.fail "INV_HOME is not a directory"
            return true
        }

        if (!invHome.exists()) {
            Logger.fail "INV_HOME does not exists"
            return true
        }

        if (!invHome.canRead()) {
            Logger.fail "current user is not able to read from INV_HOME"
            return true
        }

        return false
    }


    static void main(String[] args) {
        InvokerHelper.runScript(Main, args)
    }

}