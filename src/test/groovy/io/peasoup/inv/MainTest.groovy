package io.peasoup.inv

import org.junit.Test

class MainTest {

    @Test
    void main() {
        def script = MainTest.class.getResource("/mainTestScript.groovy")

        assert script

        Main.main(script.path)
    }

    @Test
    void main_with_pattern() {
        def script = MainTest.class.getResource("/mainTestScript.groovy")

        assert script

        def scriptParentFile = new File(script.path).parent

        Main.main(scriptParentFile + "/mainTestScript.*")
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

        /*

         */
    }

    @Test
    void main_launchScm() {
        def scmFile = MainTest.class.getResource("/.scm")

        assert scmFile

        Main.main("-s", scmFile.path)
        Main.main("--from-scm", scmFile.path)
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