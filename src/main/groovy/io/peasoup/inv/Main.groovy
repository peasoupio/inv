package io.peasoup.inv

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

        assert args[0]

        String arg0 = args[0]

        switch (arg0.toLowerCase()) {
            case "graph":
                return buildGraph(args.length > 1 ? args[1] : "plain")
            case "from-scm":
                return launchFromSCM(args[1])
            default:
                return executeScript(arg0)
        }



    }

    int executeScript(String arg0) {
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
                Logger.info("file: ${it}")
                InvInvoker.invoke(inv,new File(it))
            }
        }

        inv()

        return 0
    }

    int launchFromSCM(String arg1) {

        def invFiles = new ScmReader(new File(arg1)).execute()

        def inv = new InvDescriptor()

        invFiles.each { String name, File script ->
            Logger.info("file: ${script.canonicalPath}")
            InvInvoker.invoke(inv, script, name)
        }

        Logger.info("[SCM] done")

        inv()

        return 0
    }

    int buildGraph(String arg1) {

        System.in.newReader()

        switch (arg1.toLowerCase()) {
            case "plain" :
                new PlainGraph(System.in.newReader()).print()
                return 0
            case "dot":
                new DotGraph(System.in.newReader()).print()
                return 0
            default :
                return -1

        }
    }


    static void main(String[] args) {
        ExpandoMetaClass.enableGlobally()

        InvokerHelper.runScript(Main, args)
    }

}