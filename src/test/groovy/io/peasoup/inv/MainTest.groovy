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

}