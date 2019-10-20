package io.peasoup.inv

import groovy.cli.commons.CliBuilder
import io.peasoup.inv.graph.DeltaGraph
import io.peasoup.inv.graph.DotGraph
import io.peasoup.inv.graph.PlainGraph
import org.codehaus.groovy.runtime.InvokerHelper

class Main extends Script {

    /*
        Commands :
            inv file.groovy - Execute single groovy script. Useful for debugging
            inv pattern/*.groovy - Execute a bunch of groovy scripts based on a Ant-style file pattern. Useful for actual executions
            inv graph [plan, dot] - Print the graphdot from the logs output of a previous generation. May support futur graph format.
                                    Context usage : inv my-file.groovy | inv graph dot
            inv from-scm my-scm.file - Process the SCM file to extract or update sources
     */

    @SuppressWarnings("GroovyAssignabilityCheck")
    Object run() {

        def cli = new CliBuilder(usage:'''inv [commands]
Sequence and manage INV groovy files or logs.
Commands: 
 <file>                 Execute a single groovy file
 <pattern>              Execute an Ant-compatible file pattern
                        (p.e *.groovy, ./**/*.groovy, ...)
''')

        cli.g(
            type: String,
            longOpt:'graph',
            args:1,
            argName:'type',
            defaultValue: 'plain',
            optionalArg: true,
            'Print the graph from stdin of a previous execution')

        cli.s(
            longOpt:'from-scm',
            convert: {
                new File(it)
            },
            argName:'file',
            'Process the SCM file to extract or update sources')
        cli.d(
                longOpt:'delta',
                convert: {
                    new File(it)
                },
                argName:'file',
                'Generate a delta from a recent execution in stdin compared to a previous execution')

        cli.usage()

        def options = cli.parse(args)

        if (options.hasOption("g"))
            return buildGraph(options.g, options.arguments())

        if (options.hasOption("s"))
            return launchFromSCM(options.s, options.arguments())

        if (options.hasOption("d"))
            return delta(options.d, options.arguments())

        return executeScript( options.arguments().pop(), options.arguments())
    }

    int executeScript(String arg0, List<String> args) {
        def inv = new InvDescriptor()
        def lookupPattern = arg0
        def lookupFile = new File(lookupPattern)

        if (lookupFile.exists())
            InvInvoker.invoke(inv,lookupFile)
        else {

            def invFiles

            def invHome = System.getenv('INV_HOME')
            if (invHome) {
                invFiles = new FileNameFinder().getFileNames(
                        invHome,
                        lookupPattern, "")
            } else {

                while(!lookupFile.parentFile.exists()) {
                    lookupFile = lookupFile.parentFile
                }

                invHome = lookupFile.parent ?: "."
                invFiles = new FileNameFinder().getFileNames(
                        new File(invHome).absolutePath,
                        new File(lookupPattern).absolutePath.replace(new File(invHome).absolutePath, ""))
            }

            invFiles.each {

                InvInvoker.invoke(inv,new File(it))
            }
        }

        inv()

        return 0
    }

    int launchFromSCM(File arg1, List<String> args) {

        def invFiles = new ScmReader(arg1).execute()

        def inv = new InvDescriptor()

        invFiles.each { String name, File script ->
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
                new PlainGraph(System.in.newReader()).print()
                return 0
            case "dot":
                new DotGraph(System.in.newReader()).print()
                return 0
        }
    }

    int delta(File arg1, List<String> args) {
        new DeltaGraph(arg1.newReader(), System.in.newReader()).print()
        return 0
    }


    static void main(String[] args) {
        ExpandoMetaClass.enableGlobally()

        InvokerHelper.runScript(Main, args)
    }

}