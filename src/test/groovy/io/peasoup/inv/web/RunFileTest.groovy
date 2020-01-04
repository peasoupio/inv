package io.peasoup.inv.web

import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class RunFileTest {

    @Test
    void ctor_not_ok() {
        assertThrows(PowerAssertionError.class, {
            new RunFile(null)
        })

        assertThrows(PowerAssertionError.class, {
            new RunFile(new File("does-not-exists"))
        })
    }

    @Test
    void stageWithoutPropagate() {
        def runFile = new RunFile(new File("./src/main/example/web/run.txt"))

        assert runFile.selected.isEmpty()

        runFile.stageWithoutPropagate("my-id")

        assert runFile.selected.containsKey("my-id")
    }
}
