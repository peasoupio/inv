package io.peasoup.inv

import io.peasoup.inv.utils.Stdout
import org.junit.After
import org.junit.Ignore
import org.junit.Test

class MainTest {

    @After
    void after() {
        Logger.capture(null)
    }

    @Test
    void main_no_args() {
        Stdout.capture ({ Main.main() }, {
            assert it.contains("usage")
        })
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

        def getMainScriptTestFile = { String index = "" ->
            def script = MainTest.class.getResource("/mainTestScript${index}.groovy".toString())
            assert script

            return InvInvoker.normalizePath(new File(script.path))
        }

        def files = [
                getMainScriptTestFile(),
                getMainScriptTestFile("2")
        ]

        def scriptParentFile = new File(files[0]).parent
        Main.main(scriptParentFile + "/mainTestScript*.*")

        files.each {
            assert logs.contains("[INV] file: ${it}".toString())
        }
    }

    @Test
    void main_with_pattern_2() {
        Logger.DebugModeEnabled = true

        // Enable capture
        def logs = Logger.capture([])

        def getMainScriptTestFile = { String file ->

            def script = MainTest.class.getResource(file.toString())
            assert script

            return InvInvoker.normalizePath(new File(script.path))
        }

        def files = [
                getMainScriptTestFile("/pattern/inside/folder/mainTestScript.groovy"),
                getMainScriptTestFile("/pattern/inside/different/mainTestScript2.groovy")
        ]

        def scriptParentFile = MainTest.class.getResource("/").path
        Main.main(scriptParentFile + "/pattern/**/mainTestScript*.*")

        files.each {
            assert logs.contains("[INV] file: ${it}".toString())
        }
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