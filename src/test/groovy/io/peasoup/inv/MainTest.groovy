package io.peasoup.inv

import io.peasoup.inv.scm.ScmReader
import io.peasoup.inv.utils.Stdout
import org.junit.After
import org.junit.Test

class MainTest {

    @After
    void after() {
        Logger.capture(null)
        Logger.DebugModeEnabled = false
    }

    @Test
    void main_no_args() {
        Stdout.capture ({ Main.main() }, {
            assert it.contains("Usage")
        })
    }

    @Test
    void main() {
        // Enable capture
        def logs = Logger.capture([])

        def script = MainTest.class.getResource("/mainTestScript.groovy")
        assert script

        def canonicalPath = new File(script.path).canonicalPath

        Main.main(script.path)

        assert logs.contains("[INV] [undefined] [${canonicalPath}] [mainTestScript]".toString())
    }

    @Test
    void main_with_expansion() {
        // Enable capture
        def logs = Logger.capture([])

        def getFile = { String file ->
            def script = MainTest.class.getResource(file)
            assert script

            return new File(script.path).canonicalPath
        }

        def files = [
            getFile("/mainTestScript.groovy"),
            getFile("/mainTestScript2.groovy")
        ]

        Main.main("-e", "pattern", "test-classes/mainTestScript.groovy", "test-classes/mainTestScript2.groovy")

        assert logs.contains("[INV] [undefined] [${files[0]}] [mainTestScript]".toString())
        assert logs.contains("[INV] [undefined] [${files[1]}] [mainTestScript2]".toString())


    }

    @Test
    void main_with_pattern() {
        // Enable capture
        def logs = Logger.capture([])

        def files = [
                new File("./", "src/test/resources/mainTestScript.groovy").canonicalPath,
                new File("./", "src/test/resources/mainTestScript2.groovy").canonicalPath,
        ]

        Main.main("src/test/resources/mainTestScript*.*")

        assert logs.contains("[INV] [undefined] [${files[0]}] [mainTestScript]".toString())
        assert logs.contains("[INV] [undefined] [${files[1]}] [mainTestScript2]".toString())

    }

    @Test
    void main_with_pattern_2() {

        // Enable capture
        def logs = Logger.capture([])

        def files = [
            new File("./", "src/test/resources/pattern/inside/folder/mainTestScript.groovy").canonicalPath,
            new File("./", "src/test/resources/pattern/inside/different/mainTestScript2.groovy").canonicalPath,
        ]

        Main.main("src/test/resources/pattern/**/*.*")

        assert logs.contains("[INV] [undefined] [${files[0]}] [different-folder]".toString())
        assert logs.contains("[INV] [undefined] [${files[1]}] [different-inside]".toString())

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

        def scmFile = MainTest.class.getResource("/test-scm.groovy")
        assert scmFile

        def comparable = new ScmReader(new File(scmFile.path))
        assert comparable.scms["my-repository"]

        // Remove to make sure we trigger init
        if (comparable.scms["my-repository"].path.exists())
            comparable.scms["my-repository"].path.deleteDir()

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