package io.peasoup.inv.web


import io.peasoup.inv.utils.Stdout
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class RunFileTest {

    RunFile runFile

    @Before
    void setup() {
        runFile = new RunFile(new File("./src/main/example/web/run.txt"))
    }

    @Test
    void not_ok() {
        assertThrows(PowerAssertionError.class, {
            new RunFile(null)
        })

        assertThrows(PowerAssertionError.class, {
            new RunFile(new File("does-not-exists"))
        })
    }

    @Test
    void stageWithoutPropagate() {
        assert runFile.selected.isEmpty()

        runFile.stageWithoutPropagate("my-id")

        assert runFile.selected.containsKey("my-id")
    }

    @Test
    void propagate() {
        runFile.stageWithoutPropagate(runFile.nodes[3].value)
        assert runFile.propagate().added > 0
    }

    @Test
    void propagate_all_nodes_selected() {
        runFile.nodes.each {
            runFile.stageWithoutPropagate(it.value)
        }

        def output = runFile.propagate()

        assert output.all
        assert output.checked == runFile.nodes.size()
        assert output.added == 0
        assert output.skipped == 0
    }

    @Test
    void propagate_not_valid_id() {
        def id = "not-valid-id"

        runFile.stageWithoutPropagate(id)

        Stdout.capture ({ runFile.propagate() }, {
            assert it.contains("${id} is not a valid id")
        })
    }

    @Test
    void propagate_removed_from_queue() {
        runFile.nodes[1..-1].reverse().each {
            runFile.stageWithoutPropagate(it.value)
        }

        assert runFile.propagate().skipped > 0
    }

    @Test
    void isSelected() {
        runFile.stageWithoutPropagate("[Kubernetes] undefined")

        assert runFile.isSelected("scm4")
        assert !runFile.isSelected("scm3")
    }

    @Test
    void isSelected_not_ok() {
        assert !runFile.isSelected("not_existing")

        runFile.ownerOfScm.put("exists", ["not-exists"])

        assert !runFile.isSelected("exists")
    }
}
