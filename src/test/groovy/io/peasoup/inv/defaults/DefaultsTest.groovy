package io.peasoup.inv.defaults

import io.peasoup.inv.InvHandler
import io.peasoup.inv.InvInvoker
import io.peasoup.inv.Logger
import io.peasoup.inv.utils.Stdout
import org.junit.Test

class DefaultsTest {


    @Test
    void files() {

        Logger.DebugModeEnabled = true

        def script = DefaultsTest.class.getResource("/defaults/files/files-demo.groovy")
        def scriptFile = new File(script.path)

        ExpandoMetaClass.enableGlobally()

        def inv = new InvHandler()

        InvInvoker.invoke(inv, scriptFile)
        InvInvoker.invoke(inv, new File("./defaults/files/inv.groovy"))

        Stdout.capture ({ inv() }, {
            // GLOB All
            assert it.contains("file1-GLOB-ALL")
            assert it.contains("file2-GLOB-ALL")
            assert it.contains("files-demo.groovy-GLOB-ALL")

            // GLOB Pattern
            assert it.contains("file1-GLOB-ALL")
            assert it.contains("file2-GLOB-ALL")
            assert it.contains("files-demo.groovy-GLOB-ALL")

            // GLOB Exclude
            assert it.contains("file1-GLOB-ALL")
            assert it.contains("file2-GLOB-ALL")
            assert it.contains("files-demo.groovy-GLOB-ALL")

            // Find All
            assert it.contains("file1-FIND-ALL")
            assert it.contains("file2-FIND-ALL")
            assert it.contains("files-demo.groovy-FIND-ALL")

            // Find Pattern
            assert it.contains("file1-FIND-PATTERN")
            assert it.contains("file2-FIND-PATTERN")
            assert it.contains("files-demo.groovy-FIND-PATTERN")

            // Find Exclude
            assert it.contains("file1-FIND-PATTERN")
            assert it.contains("file2-FIND-PATTERN")
            assert !it.contains("files-demo.groovy-FIND-EXCLUDE")
        })

    }
}