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
        Main.main("graph", logOutput.path)

        println "\nTest selecting 'plain': "
        System.setIn(new ByteArrayInputStream(logOutput.bytes))
        Main.main("graph", "plain", logOutput.path)

        println "\nTest selecting 'dot': "
        System.setIn(new ByteArrayInputStream(logOutput.bytes))
        Main.main("graph", "dot", logOutput.path)
    }

    @Test
    void main_launchScm() {
        def scmFile = MainTest.class.getResource("/.scm")

        assert scmFile

        Main.main("from-scm", scmFile.path)
    }

}