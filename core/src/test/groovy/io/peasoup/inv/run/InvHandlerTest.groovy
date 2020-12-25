package io.peasoup.inv.run

import io.peasoup.inv.MissingOptionException
import io.peasoup.inv.TempHome
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import java.util.stream.Stream

import static org.junit.Assert.*

@SuppressWarnings(["GroovyAssignabilityCheck", "GroovyMissingReturnStatement", "GrEqualsBetweenInconvertibleTypes"])
@RunWith(TempHome.class)
class InvHandlerTest {

    InvExecutor executor
    InvHandler inv

    @Before
    void setup() {
        executor = new InvExecutor()
        inv = new InvHandler(executor)

        RunsRoller.latest.roll()
    }

    @Test
    void call_ok() {

        inv.call {
            name "my-webservice"

            require { Server("my-server-id") }

            broadcast { Endpoint } using {
                id name: "my-webservice-id"
                ready {
                    println "my-webservice-id has been broadcast"
                }
            }
        }

        inv.call {
            name "my-app"

            require { Endpoint } using {
                id name: "my-webservice-id"
                resolved {
                    println "my-webservice-id has been resolved by ${resolvedBy}"
                }
            }

            broadcast { App("my-app-id") }
        }

        inv.call {
            name "my-server"

            broadcast { Server } using {
                id "my-server-id"
                ready {
                    println "my-server-id has been broadcast"
                }
            }
        }


        def report = executor.execute()

        assertTrue report.isOk()

        assertEquals 3, report.digested.size()
        assertEquals "my-server", report.digested[0].name
        assertEquals "my-webservice", report.digested[1].name
        assertEquals "my-app", report.digested[2].name
    }

    @Test
    void not_ok() {
        assertThrows(IllegalArgumentException.class, {
            new InvHandler(null)
        })
    }

    @Test
    void call_not_ok() {
        assertThrows(IllegalArgumentException.class, {
            inv.call(null)
        })
    }

    @Test
    void call_without_name() {
        assertThrows(MissingOptionException.class, {
            inv.call({}, '')
        })
    }

    @Test
    void call_using_variable_id() {
        inv.call {
            def myId = [id: 1]

            broadcast { Something(myId) }

            require { Something(myId) }
        }

        def report = executor.execute()
        assertTrue report.isOk()
    }


    @Test
    void call_using_delayed_id() {
        inv.call {
            def myId = [id: 1]

            broadcast { Something } using {
                ready { myId }
            }

            require { Something }

            broadcast { Something { $something.id } }

            require { Something { $something.id } }
        }

        def report = executor.execute()
        assertTrue report.isOk()
    }

    @Test
    void call_using_delayed_id_with_bad_variable() {
        inv.call {
            def myId = [id: 1]

            broadcast { Something } using {
                ready { myId }
            }

            require { Something }

            broadcast { Something { $SomethingElse.id } }

            require { Something { $SomethingElse.id } }
        }

        def report = executor.execute()
        assertFalse report.isOk()
    }

    @Test
    void call_broadcast_twice() {
        inv.call {
            name "my-webservice"

            require { Server("my-server-id") }

            broadcast { Endpoint } using {
                id "my-webservice-id"
                ready {
                    println "my-webservice-id has been broadcast"
                }
            }
        }

        inv.call {
            name "my-webservice-2"

            require { Server("my-server-id") }

            broadcast { Endpoint } using {
                id "my-webservice-id"
                ready {
                    println "my-webservice-id has been broadcast twice"
                }
            }
        }

        inv.call {
            name "my-app"

            require { Endpoint } using {
                id "my-webservice-id"
            }

            broadcast { App("my-app-id") }
        }

        inv.call {
            name "my-server"

            broadcast { Server } using {
                id "my-server-id"
                ready {
                    println "my-server-id has been broadcast"
                }
            }
        }


        def report = executor.execute()
        assertTrue report.isOk()

        assertTrue report.digested
                .findAll { it.name.contains("my-webservice") }
                .collectMany { it.totalStatements }
                .any {
                    it.state == StatementStatus.ALREADY_BROADCAST
                }
    }

    @Test
    void call_using_step() {
        inv.call {
            name "my-webservice"

            require { Server("my-server-id") }

            step {
                broadcast { Endpoint } using {
                    id "my-webservice-id"
                    ready {
                        return "http://my.endpoint.com"
                    }
                }
            }
        }

        inv.call {
            name "my-app"

            require { Endpoint("my-webservice-id") } into '$ep'

            step {
                broadcast { App("my-app-id") } using {
                    ready {
                        print "My App is hosted here: ${$ep}"
                    }
                }
            }
        }

        inv.call {
            name "my-server"

            broadcast { Server } using {
                id "my-server-id"
                ready {
                    println "my-server-id has been broadcast"
                }
            }
        }

        def report = executor.execute()
        assertTrue report.isOk()
    }

    @Test
    void call_using_step_and_unbloating() {
        inv.call {
            name "my-webservice"


            require { Server("my-server-id") }

            step {
                broadcast { Endpoint } using {
                    id "my-webservice-id"
                    ready {
                        return "http://my.endpoint.com"
                    }
                }
            }
        }

        inv.call {
            name "my-app"

            require { Endpoint("my-unbloatable-ws-id") } using {
                unbloatable true
            }

            require { Endpoint("my-webservice-id") } into '$ep'

            step {
                broadcast { App("my-app-id") } using {
                    ready {
                        print "My App is hosted here: ${$ep}"
                    }
                }
            }
        }

        inv.call {
            name "my-server"

            broadcast { Server } using {
                id "my-server-id"
                ready {
                    println "my-server-id has been broadcast"
                }
            }
        }

        def report = executor.execute()
        assertTrue report.isOk()
    }

    @Test
    void call_using_step_unbloating_and_broadcast_after() {
        inv.call {
            name "my-webservice"


            require { Server("my-server-id") }

            step {
                broadcast { Endpoint } using {
                    id "my-webservice-id"
                    ready {
                        return "http://my.endpoint.com"
                    }
                }
            }
        }

        inv.call {
            name "my-app"

            require { Endpoint("my-unbloatable-ws-id") } using {
                unbloatable true
            }

            step {
                broadcast { App("my-app-id") }
            }
        }

        inv.call {
            name "my-server"

            broadcast { Server } using {
                id "my-server-id"
                ready {
                    println "my-server-id has been broadcast"
                }
            }
        }

        inv.call {
            name "my-other-app"

            step {
                require { App("my-app-id") }
            }

            step {
                require { Element("not-existing") } using { unbloatable true }
            }

            step {
                broadcast { Element("at-the-end") }
            }
        }

        def report = executor.execute()
        assertTrue report.isOk()
    }

    @Test
    void call_using_multiple_unbloating() {
        inv.call {
            name "my-app-1"

            require { Artifact("A") } using { unbloatable true }
            broadcast { Artifact("B") }
            require { Artifact("B") } using { unbloatable true }
            require { Artifact("C") } using { unbloatable true }
            require { Artifact("D") } using { unbloatable true }

            require { Service("A") } using { unbloatable true }
            require { Service("B") }
            require { Service("C") } using { unbloatable true }
            require { Service("D") } using { unbloatable true }
        }

        inv.call {
            name "my-app-2"

            require { Artifact("A") } using { unbloatable true }
            require { Artifact("B") }
            require { Artifact("C") } using { unbloatable true }
            require { Artifact("D") } using { unbloatable true }

            require { Service("A") } using { unbloatable true }
            broadcast { Service("B") }
            require { Service("C") } using { unbloatable true }
            require { Service("D") } using { unbloatable true }
        }

        def report = executor.execute()

        assertTrue report.isOk()
        assertEquals 8, report.cycleCount
    }

    @Test
    void ready() {

        def value = 1

        inv.call {
            name "my-webservice"

            require { Server("my-server-id") }

            step {
                broadcast { Endpoint } using {
                    id "my-webservice-id"
                    ready {
                        value++
                        return "http://my.endpoint.com"
                    }
                }
            }

            ready {
                assertEquals 1, value
            }
        }

        inv.call {
            name "my-app"

            require { Endpoint("my-webservice-id") } into '$ep'

            step {
                broadcast { App("my-app-id") } using {
                    ready {
                        value++
                        print "My App is hosted here: ${$ep}"
                    }
                }
            }
        }

        inv.call {
            name "my-server"

            broadcast { Server } using {
                id "my-server-id"
                ready {
                    value++
                    println "my-server-id has been broadcast"
                }
            }
        }

        def report = executor.execute()
        assertTrue report.isOk()
    }

    @Test
    void call_using_parallelism_on_creation() {

        def patternProvider = { provider ->
            this.inv.call {
                name "element-${provider}"
                broadcast { Element(provider) }
            }
        }

        def patternConsumer = { provider, consumer ->
            this.inv.call {
                name "element-${consumer}"
                require { Element(provider) }
                step { broadcast { Element(consumer) } }
            }
        }

        Stream.of("A", "B", "C", "D", "E")
                .parallel()
                .each { provider ->
                    patternProvider(provider)

                    Stream.of("A", "B", "C", "D", "E")
                            .parallel()
                            .each { consumer ->
                                patternConsumer(provider, provider + consumer)
                            }
                }

        def report = executor.execute()
        assertTrue report.isOk()
    }

    @Test
    void call_with_halting() {

        inv.call {
            name "my-webservice"

            require { Server("my-server-id") }

            broadcast { Endpoint } using {
                id "my-webservice-id"
                ready {
                    println "my-webservice-id has been broadcast"
                }
            }
        }

        inv.call {
            name "my-server"

            broadcast { Server } using {
                id "my-server-id"
                ready {
                    println "my-server-id has been broadcast"
                }
            }
        }

        inv.call {
            name "my-app"

            require { Endpoint("my-webservice-id-not-existing") }

            broadcast { App("my-app-id") }
        }

        inv.call {
            name "my-app-2"

            require { Endpoint("my-webservice-id-not-existing") }
            require { App("my-app-id") }

            require { Endpoint("my-unbloatable-endpoint") } using {
                unbloatable true
            }

            broadcast { App("my-app-id-2") }
        }

        inv.call {
            name "my-app-3"

            require { Endpoint("my-webservice-id-not-existing") }
            require { App("my-app-id") }
            require { App("my-app-id-2") }

            require { Endpoint("my-webservice-id") }

            broadcast { App("my-app-id-3") }
        }


        def report = executor.execute()
        assertFalse report.isOk()
        assertTrue report.halted
        assertEquals 2, report.digested.size()
        assertEquals "my-server", report.digested[0].name
        assertEquals "my-webservice", report.digested[1].name
    }

    @Test
    void call_with_exception() {
        inv.call {
            name "my-exception"

            throw new Exception("fail")
        }

        def report = executor.execute()
        assertFalse report.isOk()
        assertFalse report.errors.isEmpty()
        assertNotNull report.errors.find { it.inv.name == "my-exception" }
        assertNotNull report.errors.find { it.throwable.message == "fail" }
    }

    @Test
    void call_with_exception_2() {
        inv.call {
            name "my-exception"

            broadcast { MyException } using {
                ready {
                    throw new Exception("fail-broadcast")
                }
            }
        }

        def report = executor.execute()
        assertFalse report.isOk()
        assertFalse report.errors.isEmpty()
        assertNotNull report.errors.find { it.throwable.message == "fail-broadcast" }
    }

    @Test
    void call_with_exception_3() {
        inv.call {
            name "provide"

            broadcast { Something }
        }

        inv.call {
            name "my-exception"

            require { Something } using {
                resolved {
                    throw new Exception("fail-require")
                }
            }
        }

        def report = executor.execute()
        assertFalse report.isOk()
        assertFalse report.errors.isEmpty()
        assertNotNull report.errors.find { it.throwable.message == "fail-require" }
    }

    @Test
    void call_with_exception_4() {

        inv.call {
            name "provide"

            broadcast { Something }
        }

        inv.call {
            name "my-exception"

            require { Something } using {
                resolved {
                    assertTrue 1 == 2, "Does not equal"
                }
            }
        }

        def report = executor.execute()
        assertFalse report.isOk()
        assertFalse report.errors.isEmpty()
    }

    @Test
    void pop_and_tail() {
        int index = 0

        inv.call {
            name "3"

            pop false
            tail true

            broadcast { Something("3") } using {
                ready {
                    assertEquals 3, index
                }
            }
        }


        inv.call {
            name "2"

            pop false
            tail true

            broadcast { Something("2") } using {
                ready {
                    assertEquals 2, index
                    index++
                }
            }
        }

        inv.call {
            name "1"

            pop false
            tail false

            broadcast { Something("1") } using {
                ready {
                    assertEquals 1, index
                    index++
                }
            }
        }

        inv.call {
            name "0"

            pop true
            tail false

            broadcast { Something("0") } using {
                ready {
                    assertEquals 0, index
                    index++
                }
            }
        }

        def remainingInvs = executor.pool.sort()

        assertEquals "0", remainingInvs[0].name
        assertEquals "1", remainingInvs[1].name
        assertEquals "2", remainingInvs[2].name
        assertEquals "3", remainingInvs[3].name
    }

    @Test
    void tags_ok() {
        inv.call {
            name "3"

            tags(
                    my: 'tag'
            )

            step {
                assertNotNull tags.my
                assertEquals 'tag', tags.my
            }
        }

        def report = executor.execute()
        assertTrue report.isOk()
    }

    @Test
    void when_ok() {

        boolean reached = false

        inv.call {
            name "1"

            tags(
                    my: 'tag'
            )
        }

        inv.call {
            name "2"

            when all tags(my: 'tag') completed {
                reached = true
            }
        }

        def report = executor.execute()
        assertTrue report.isOk()
        assertTrue reached
    }

    @Test
    void when_ok_created() {

        boolean reached = false

        inv.call {
            name "1"

            tags(
                    my: 'tag'
            )

            require { Something }
        }

        inv.call {
            name "2"

            when all tags(my: 'tag') created {
                reached = true
            }
        }

        def report = executor.execute()
        assertFalse report.isOk()
        assertTrue reached
    }

    @Test
    void when_ok_completed() {

        boolean reached = false

        inv.call {
            name "1"

            broadcast { Something }
        }

        inv.call {
            name "2"

            tags(
                    my: 'tag'
            )

            require { Something }
        }

        inv.call {
            name "3"

            when all tags(my: 'tag') completed {
                reached = true
            }
        }

        def report = executor.execute()
        assertTrue report.isOk()
        assertTrue reached
    }

    @Test
    void when_tag_not_ok() {

        boolean reached = false

        inv.call {
            name "1"

            tags(
                    my: 'tag'
            )
        }

        inv.call {
            name "2"

            when all tags(my: 'other-tag') completed {
                reached = true
            }
        }

        def report = executor.execute()
        assertFalse report.isOk()
        assertFalse reached
    }

    @Test
    void when_name_ok() {

        boolean reached = false

        inv.call {
            name "1"

            tags(
                    my: 'tag'
            )
        }

        inv.call {
            name "2"

            when all name "1" completed {
                reached = true
            }
        }

        def report = executor.execute()
        assertTrue report.isOk()
        assertTrue reached
    }

    @Test
    void when_name_ok_contains() {

        boolean reached = false

        inv.call {
            name "123"

            tags(
                    my: 'tag'
            )
        }

        inv.call {
            name "2"

            when all name "1" completed {
                reached = true
            }
        }

        def report = executor.execute()
        assertTrue report.isOk()
        assertTrue reached
    }

    @Test
    void when_name_not_ok() {

        boolean reached = false

        inv.call {
            name "1"

            tags(
                    my: 'tag'
            )
        }

        inv.call {
            name "2"

            when all name "2" completed {
                reached = true
            }
        }

        def report = executor.execute()
        assertFalse report.isOk()
        assertFalse reached
    }

    @Test
    void when_ok_broadcasts() {

        inv.call {
            name "1"

            tags(
                    my: 'tag'
            )
        }

        inv.call {
            name "2"

            when all tags(my: 'tag') completed {
                step {
                    broadcast { Something }
                }
            }
        }

        inv.call {
            name "3"

            require { Something }

            broadcast { Else }
        }

        inv.call {
            name "4"

            require { Else }
        }

        def report = executor.execute()
        assertTrue report.isOk()
    }

    @Test
    void when_ok_require() {

        boolean raised = false

        inv.call {
            name "1"

            tags(
                    my: 'tag'
            )

            broadcast { Something }
        }

        inv.call {
            name "2"

            tags(
                    my: 'tag'
            )

            require { Something }

            when all tags(my: 'tag') completed {
                raised = true
            }
        }

        def report = executor.execute()
        assertTrue report.isOk()
        assertTrue raised
    }

    @Test
    void when_error_require() {

        boolean raised = false

        inv.call {
            name "1"

            tags(
                    my: 'tag'
            )

            broadcast { Something }
        }

        inv.call {
            name "2"

            tags(
                    my: 'tag'
            )

            require { Else }

            when all tags(my: 'tag') completed {
                raised = true
            }
        }

        def report = executor.execute()
        assertFalse report.isOk()
        assertFalse raised
    }

    @Test
    void multiple_steps() {

        def size = 10
        def steps = []
        1.upto(size, { final Number reference ->
            steps << {
                assertEquals it + 1, reference
            }
        })

        inv.call {

            name "steps"

            for (Closure body : steps) {
                step(body)
            }
        }
        def report = executor.execute()
        assertTrue report.isOk()
    }

    @Test
    void multiple_steps_require_and_broadcasts() {
        inv.call {
            name "step1"

            step {
                assertEquals 0, it

                require { Something }
            }

            step {
                assertEquals 1, it

                broadcast { Else }
                require { More }
            }

            step {
                assertEquals 2, it

                broadcast { Final }
            }
        }

        inv.call {
            name "step2"

            step {
                assertEquals 0, it

                broadcast { Something }
            }

            step {
                assertEquals 1, it

                require { Else }
                broadcast { More }
            }

            step {
                assertEquals 2, it

                require { Final }
            }
        }

        def report = executor.execute()
        assertTrue report.isOk()
    }

    @Test
    void ok_doc() {
        inv.call {
            name "doc"
            markdown '''
This is a sample description for this INV.
'''

            broadcast { Something } using {
                markdown '''
This is a sample description for this  
broadcast statement
'''
            }

            require { Something } using {
                markdown '''
This is a sample description for **this** require statement
'''
            }

            broadcast { Else } using {
                markdown '''
This is a sample description for **this** broadcast statement
'''
            }
        }

        def report = executor.execute()
        assertTrue report.isOk()

        def reportFolder = new File(RunsRoller.latest.folder(), "reports")
        assertTrue reportFolder.exists()
        assertTrue new File(reportFolder, "doc.md").exists()
    }
}