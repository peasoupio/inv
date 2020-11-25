package io.peasoup.inv.composer

import io.peasoup.inv.TempHome
import io.peasoup.inv.utils.Stdout
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(TempHome.class)
class RunFileTest {

    RunFile runFile

    @Before
    void setup() {
        runFile = new RunFile(new File("../examples/composer/run.txt"))
    }

    @Test
    void not_ok() {
        assertThrows(AssertionError.class, {
            new RunFile(null)
        })

        assertThrows(AssertionError.class, {
            new RunFile(new File("does-not-exists"))
        })

        assertThrows(AssertionError.class, {
            runFile.stage(null)
        })

        assertThrows(AssertionError.class, {
            runFile.stage('')
        })

        assertThrows(AssertionError.class, {
            runFile.stageWithoutPropagate(null)
        })

        assertThrows(AssertionError.class, {
            runFile.stageWithoutPropagate('')
        })

        assertThrows(AssertionError.class, {
            runFile.unstage(null)
        })

        assertThrows(AssertionError.class, {
            runFile.unstage('')
        })
    }

    @Test
    void stageWithoutPropagate() {
        assertTrue runFile.staged.isEmpty()

        runFile.stageWithoutPropagate("my-id")
        runFile.stageWithoutPropagate("my-id") // can handle twice

        assertTrue runFile.staged.containsKey("my-id")
    }

    @Test
    void unstage() {
        String myId = "my-id"

        runFile.stageWithoutPropagate(myId)
        assertTrue runFile.staged.containsKey(myId)

        runFile.unstage(myId)
        runFile.unstage(myId) // can handle twice

        assertFalse runFile.staged.containsKey(myId)
    }

    @Test
    void propagate() {
        runFile.stageWithoutPropagate(runFile.nodes[3].value)
        assertTrue runFile.propagate().added > 0
    }

    @Test
    void propagate_reverse() {
        runFile.nodes.toList()[1..6].each {
            runFile.stageWithoutPropagate(it.value)
        }

        assertEquals 1, runFile.propagate().added
    }

    @Test
    void propagate_all_nodes_selected() {
        runFile.nodes.each {
            runFile.stageWithoutPropagate(it.value)
        }

        def output = runFile.propagate()

        assertNotNull output.all
        assertEquals  runFile.nodes.size(), output.checked
        assertEquals 0, output.added
    }

    @Test
    void propagate_not_valid_id() {
        def id = "not-valid-id"

        runFile.stageWithoutPropagate(id)

        Stdout.capture ({ runFile.propagate() }, {
            assertTrue it.contains("${id} is not a valid id")
        })
    }

    /*
    @Test
    void propagate_removed_from_queue() {
        runFile.nodes[1..-2].reverse().each {
            runFile.stageWithoutPropagate(it.value)
        }

        assertTrue runFile.propagate().skipped > 0
    }
     */

    @Test
    void isSelected() {
        runFile.stageWithoutPropagate("[Kubernetes] undefined")

        assertTrue runFile.isSelected("repo4")
        assertFalse runFile.isSelected("repo3")
    }

    @Test
    void selectedRepos() {
        runFile.stageWithoutPropagate("does-not-exists")
        runFile.stageWithoutPropagate("[Maven] undefined") // does not have a file statement
        runFile.stageWithoutPropagate("[Artifact] com.mycompany.app:my-app-1") // undefined repo
        runFile.stageWithoutPropagate("[Kubernetes] undefined")

        assertNotNull runFile.selectedRepos()
        assertEquals 2, runFile.selectedRepos().size()
        assertNotNull runFile.selectedRepos().find { it == "repo4" } // from Kubernetes
        assertNotNull runFile.selectedRepos().find { it == "undefined" } // from Artifact
    }

    @Test
    void isSelected_not_ok() {
        assertFalse runFile.isSelected("not_existing")

        runFile.ownerOfRepo.put("exists", ["not-exists"])

        assertFalse runFile.isSelected("exists")
    }
}
