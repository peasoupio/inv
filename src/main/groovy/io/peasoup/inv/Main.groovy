package io.peasoup.inv

import org.codehaus.groovy.runtime.InvokerHelper

class Main extends Script {

    def run() {
        assert args[0]

        ExpandoMetaClass.enableGlobally()

        def inv = new InvDescriptor()
        def lookupPattern = args[0]
        def lookupFile = new File(lookupPattern)


        if (lookupFile.exists())
            InvInvoker.invoke(inv,lookupPattern)
        else {
            def invHome = System.getenv('INV_HOME') ?: ""
            def invFiles = new FileNameFinder().getFileNames(invHome, lookupPattern)

            invFiles.each {
                InvInvoker.invoke(inv,it)
            }
        }

        inv()
    }



    static void main(String[] args) {
        InvokerHelper.runScript(Main, args)
    }

}