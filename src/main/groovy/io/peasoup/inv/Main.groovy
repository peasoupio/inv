package io.peasoup.inv

import org.codehaus.groovy.runtime.InvokerHelper

class Main extends Script {

    @SuppressWarnings("GroovyAssignabilityCheck")
    def run() {
        assert args[0]

        ExpandoMetaClass.enableGlobally()

        def inv = new InvDescriptor()
        def lookupPattern = (String)args[0]
        def lookupFile = new File(lookupPattern)

        if (lookupFile.exists())
            InvInvoker.invoke(inv,lookupFile.absolutePath)
        else {
            def invHome = System.getenv('INV_HOME') ?: (lookupFile.parent ?: ".")
            def invFiles = new FileNameFinder().getFileNames(new File(invHome).absolutePath, lookupFile.name)

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