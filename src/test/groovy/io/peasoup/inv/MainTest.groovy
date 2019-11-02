package io.peasoup.inv

import io.peasoup.inv.utils.Stdout
import org.junit.After
import org.junit.Test

class MainTest {

    @After
    void after() {
        Logger.capture(null)
    }

    @Test
    void main() {
        // Enable capture
        def logs = Logger.capture([])

        def script = MainTest.class.getResource("/mainTestScript.groovy")
        assert script

        def canonicalPath = InvInvoker.normalizePath(new File(script.path))

        Main.main(script.path)

        assert logs.contains("[INV] file: ${canonicalPath}".toString())
    }

    @Test
    void main_with_pattern() {
        // Enable capture
        def logs = Logger.capture([])

        def script = MainTest.class.getResource("/mainTestScript.groovy")
        assert script

        def scriptParentFile = new File(script.path).parent
        def canonicalPath = InvInvoker.normalizePath(new File(script.path))

        Main.main(scriptParentFile + "/mainTestScript.*")

        assert logs.contains("[INV] file: ${canonicalPath}".toString())
    }

    @Test
    void main_graph() {
        def logOutput = MainTest.class.getResource("/logOutput1.txt")

        assert logOutput

        println "\nTest default graph: "
        System.setIn(new ByteArrayInputStream(logOutput.bytes))
        Main.main("-g")

        println "\nTest default graph: "
        System.setIn(new ByteArrayInputStream(logOutput.bytes))
        Main.main("--graph")

        println "\nTest selecting 'plain': "
        System.setIn(new ByteArrayInputStream(logOutput.bytes))
        Main.main("--graph", "plain")

        println "\nTest selecting 'dot': "
        System.setIn(new ByteArrayInputStream(logOutput.bytes))
        Main.main("--graph", "dot")
    }

    @Test
    void main_launchScm() {
        Logger.DebugModeEnabled = true

        def scmFile = MainTest.class.getResource("/test.scm")
        assert scmFile

        def comparable = new ScmReader(new File(scmFile.path).newReader())
        assert comparable.scm["my-repository"]

        // Remove to make sure we trigger init
        if (comparable.scm["my-repository"].path.exists())
            comparable.scm["my-repository"].path.deleteDir()

        Stdout.capture ({ Main.main("-s", scmFile.path) }, {
            assert it.contains("init")
        })

        Stdout.capture ({ Main.main("--from-scm", scmFile.path) }, {
            assert it.contains("update")
        })
    }

    @Test
    void main_delta() {

        def logOutput = MainTest.class.getResource("/logOutput1.txt")
        def logOutputAfter = MainTest.class.getResource("/logAfterOutput1.txt")

        assert logOutput
        assert logOutputAfter

        println "\nTest selecting 'delta': "
        System.setIn(new ByteArrayInputStream(logOutputAfter.bytes))
        Main.main("-d", logOutput.path)

        System.setIn(new ByteArrayInputStream(logOutputAfter.bytes))
        Main.main("--delta", logOutput.path)
    }

}