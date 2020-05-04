package io.peasoup.inv.run

import io.peasoup.inv.TempHome
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import java.util.stream.Stream

import static org.junit.jupiter.api.Assertions.assertThrows

@RunWith(TempHome.class)
class InvHandlerTest {

    InvExecutor executor
    InvHandler inv

    @Before
    void setup() {
        executor = new InvExecutor()
        inv = new InvHandler(executor)
    }

    @Test
    void call_ok() {

        inv {
            name "my-webservice"

            require inv.Server("my-server-id")

            broadcast inv.Endpoint using {
                id name: "my-webservice-id"
                ready {
                    println "my-webservice-id has been broadcast"
                }
            }
        }

        inv {
            name "my-app"

            require inv.Endpoint using {
                id name: "my-webservice-id"
                resolved {
                    println "my-webservice-id has been resolved by ${resolvedBy}"
                }
            }

            broadcast inv.App("my-app-id")
        }

        inv {
            name "my-server"

            broadcast inv.Server using {
                id "my-server-id"
                ready {
                    println "my-server-id has been broadcast"
                }
            }
        }


        def report = executor.execute()

        assert report.isOk()

        assert report.digested.size() == 3
        assert report.digested[0].name == "my-server"
        assert report.digested[1].name == "my-webservice"
        assert report.digested[2].name == "my-app"
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
        assertThrows(InvHandler.INVOptionRequiredException.class, {
            inv.call({}, '')
        })
    }

    @Test
    void call_broadcast_twice() {

        inv {
            name "my-webservice-2"

            require inv.Server("my-server-id")

            broadcast inv.Endpoint using {
                id "my-webservice-id"
                ready {
                    println "my-webservice-id has been broadcast"
                }
            }
        }

        inv {
            name "my-webservice"

            require inv.Server("my-server-id")

            broadcast inv.Endpoint using {
                id "my-webservice-id"
                ready {
                    println "my-webservice-id has been broadcast twice"
                }
            }
        }

        inv {
            name "my-app"

            require inv.Endpoint using {
                id "my-webservice-id"
            }

            broadcast inv.App("my-app-id")
        }

        inv {
            name "my-server"

            broadcast inv.Server using {
                id "my-server-id"
                ready {
                    println "my-server-id has been broadcast"
                }
            }
        }


        def report = executor.execute()
        assert report.isOk()

        report.digested
                .findAll { it.name.contains("my-webservice") }
                .collect { it.totalStatements }
                .any {
                    it.state == StatementStatus.ALREADY_BROADCAST
                }
    }

    @Test
    void call_using_step() {

        inv {
            name "my-webservice"

            require inv.Server("my-server-id")

            step {
                broadcast inv.Endpoint using {
                    id "my-webservice-id"
                    ready {
                        return "http://my.endpoint.com"
                    }
                }
            }
        }

        inv {
            name "my-app"

            require inv.Endpoint("my-webservice-id") into '$ep'

            step {
                broadcast inv.App("my-app-id") using {
                    ready {
                        print "My App is hosted here: ${$ep}"
                    }
                }
            }
        }

        inv {
            name "my-server"

            broadcast inv.Server using {
                id "my-server-id"
                ready {
                    println "my-server-id has been broadcast"
                }
            }
        }

        def report = executor.execute()
        assert report.isOk()
    }

    @Test
    void call_using_step_and_unbloating() {


        inv {
            name "my-webservice"


            require inv.Server("my-server-id")

            step {
                broadcast inv.Endpoint using {
                    id "my-webservice-id"
                    ready {
                        return "http://my.endpoint.com"
                    }
                }
            }
        }

        inv {
            name "my-app"

            require inv.Endpoint("my-unbloatable-ws-id") using {
                unbloatable true
            }

            require inv.Endpoint("my-webservice-id") into '$ep'

            step {
                broadcast inv.App("my-app-id") using {
                    ready {
                        print "My App is hosted here: ${$ep}"
                    }
                }
            }
        }

        inv {
            name "my-server"

            broadcast inv.Server using {
                id "my-server-id"
                ready {
                    println "my-server-id has been broadcast"
                }
            }
        }

        def report = executor.execute()
        assert report.isOk()
    }

    @Test
    void call_using_step_unbloating_and_broadcast_after() {


        inv {
            name "my-webservice"


            require inv.Server("my-server-id")

            step {
                broadcast inv.Endpoint using {
                    id "my-webservice-id"
                    ready {
                        return "http://my.endpoint.com"
                    }
                }
            }
        }

        inv {
            name "my-app"

            require inv.Endpoint("my-unbloatable-ws-id") using {
                unbloatable true
            }

            step {
                broadcast inv.App("my-app-id")
            }
        }

        inv {
            name "my-server"

            broadcast inv.Server using {
                id "my-server-id"
                ready {
                    println "my-server-id has been broadcast"
                }
            }
        }

        inv {
            name "my-other-app"

            step {
                require inv.App("my-app-id")
            }

            step {
                require inv.Element("not-existing") using { unbloatable true }
            }

            step {
                broadcast inv.Element("at-the-end")
            }
        }

        def report = executor.execute()
        assert report.isOk()
    }

    @Test
    void call_using_multiple_unbloating() {


        inv {
            name "my-app-1"

            require inv.Artifact("A") using { unbloatable true }
            broadcast inv.Artifact("B")
            require inv.Artifact("B") using { unbloatable true }
            require inv.Artifact("C") using { unbloatable true }
            require inv.Artifact("D") using { unbloatable true }

            require inv.Service("A") using { unbloatable true }
            require inv.Service("B")
            require inv.Service("C") using { unbloatable true }
            require inv.Service("D") using { unbloatable true }
        }

        inv {
            name "my-app-2"

            require inv.Artifact("A") using { unbloatable true }
            require inv.Artifact("B")
            require inv.Artifact("C") using { unbloatable true }
            require inv.Artifact("D") using { unbloatable true }

            require inv.Service("A") using { unbloatable true }
            broadcast inv.Service("B")
            require inv.Service("C") using { unbloatable true }
            require inv.Service("D") using { unbloatable true }
        }

        def report = executor.execute()

        assert report.isOk()
        assert report.cycleCount == 8
    }

    @Test
    void ready() {

        def value = 1

        inv {
            name "my-webservice"

            require inv.Server("my-server-id")

            step {
                broadcast inv.Endpoint using {
                    id "my-webservice-id"
                    ready {
                        value++
                        return "http://my.endpoint.com"
                    }
                }
            }

            ready {
                assert value == 1
            }
        }

        inv {
            name "my-app"

            require inv.Endpoint("my-webservice-id") into '$ep'

            step {
                broadcast inv.App("my-app-id") using {
                    ready {
                        value++
                        print "My App is hosted here: ${$ep}"
                    }
                }
            }
        }

        inv {
            name "my-server"

            broadcast inv.Server using {
                id "my-server-id"
                ready {
                    value++
                    println "my-server-id has been broadcast"
                }
            }
        }

        def report = executor.execute()
        assert report.isOk()
    }

    @Test
    void call_using_parallelism_on_creation() {

        def patternProvider = { provider ->
            this.inv.call {
                name "element-${provider}"
                broadcast inv.Element(provider)
            }
        }

        def patternConsumer = { provider, consumer ->
            this.inv.call {
                name "element-${consumer}"
                require inv.Element(provider)
                step { broadcast inv.Element(consumer) }
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
        assert report.isOk()
    }

    @Test
    void call_with_halting() {

        assertThrows(IllegalArgumentException.class, {
            inv.call(null)
        })

        inv {
            name "my-webservice"

            require inv.Server("my-server-id")

            broadcast inv.Endpoint using {
                id "my-webservice-id"
                ready {
                    println "my-webservice-id has been broadcast"
                }
            }
        }

        inv {
            name "my-server"

            broadcast inv.Server using {
                id "my-server-id"
                ready {
                    println "my-server-id has been broadcast"
                }
            }
        }

        inv {
            name "my-app"

            require inv.Endpoint("my-webservice-id-not-existing")

            broadcast inv.App("my-app-id")
        }

        inv {
            name "my-app-2"

            require inv.Endpoint("my-webservice-id-not-existing")
            require inv.App("my-app-id")

            require inv.Endpoint("my-unbloatable-endpoint") using {
                unbloatable true
            }

            broadcast inv.App("my-app-id-2")
        }

        inv {
            name "my-app-3"

            require inv.Endpoint("my-webservice-id-not-existing")
            require inv.App("my-app-id")
            require inv.App("my-app-id-2")

            require inv.Endpoint("my-webservice-id")

            broadcast inv.App("my-app-id-3")
        }


        def report = executor.execute()
        assert !report.isOk()

        assert report.halted
        assert report.digested.size() == 2
        assert report.digested[0].name == "my-server"
        assert report.digested[1].name == "my-webservice"
    }

    @Test
    void call_with_exception() {

        inv {
            name "my-exception"

            throw new Exception("fail")
        }

        def report = executor.execute()
        assert !report.isOk()

        assert !report.errors.isEmpty()
        assert report.errors.find { it.inv.name == "my-exception" }
        assert report.errors.find { it.throwable.message == "fail" }
    }

    @Test
    void call_with_exception_2() {

        inv {
            name "my-exception"

            broadcast inv.Exception using {
                ready {
                    throw new Exception("fail-broadcast")
                }
            }
        }

        def report = executor.execute()
        assert !report.isOk()

        assert !report.errors.isEmpty()
        assert report.errors.find { it.throwable.message == "fail-broadcast" }
    }

    @Test
    void call_with_exception_3() {

        inv {
            name "provide"

            broadcast inv.Something
        }

        inv {
            name "my-exception"

            require inv.Something using {
                resolved {
                    throw new Exception("fail-require")
                }
            }
        }

        def report = executor.execute()
        assert !report.isOk()

        assert !report.errors.isEmpty()
        assert report.errors.find { it.throwable.message == "fail-require" }
    }

    @Test
    void call_with_exception_4() {

        inv {
            name "provide"

            broadcast inv.Something
        }

        inv {
            name "my-exception"

            require inv.Something using {
                resolved {
                    assert 1 == 2, "Does not equal"
                }
            }
        }

        def report = executor.execute()
        assert !report.isOk()

        assert !report.errors.isEmpty()
    }

    @Test
    void pop_and_tail() {

        int index = 0

        inv {
            name "3"

            pop false
            tail true

            broadcast inv.Something("3") using {
                ready {
                    assert index == 3
                }
            }
        }


        inv {
            name "2"

            pop false
            tail true

            broadcast inv.Something("2") using {
                ready {
                    assert index == 2
                    index++
                }
            }
        }

        inv {
            name "1"

            pop false
            tail false

            broadcast inv.Something("1") using {
                ready {
                    assert index == 1
                    index++
                }
            }
        }

        inv {
            name "0"

            pop true
            tail false

            broadcast inv.Something("0") using {
                ready {
                    assert index == 0
                    index++
                }
            }
        }

        def remainingInvs = executor.pool.sortRemainingInvs()

        assert remainingInvs[0].name == "0"
        assert remainingInvs[1].name == "1"
        assert remainingInvs[2].name == "2"
        assert remainingInvs[3].name == "3"
    }

    @Test
    void tags_ok() {
        inv {
            name "3"

            tags(
                    my: 'tag'
            )

            step {
                assert tags.my
                assert tags.my == 'tag'
            }
        }

        def report = executor.execute()
        assert report.isOk()
    }

    @Test
    void when_ok() {

        boolean reached = false

        inv {
            name "1"

            tags(
                    my: 'tag'
            )
        }

        inv {
            name "2"

            when all tags(my: 'tag') completed {
                reached = true
            }
        }

        def report = executor.execute()
        assert report.isOk()
        assert reached
    }

    @Test
    void when_ok_created() {

        boolean reached = false

        inv {
            name "1"

            tags(
                    my: 'tag'
            )

            require inv.Something
        }

        inv {
            name "2"

            when all tags(my: 'tag') created {
                reached = true
            }
        }

        executor.execute()
        assert reached
    }

    @Test
    void when_ok_completed() {

        boolean reached = false

        inv {
            name "1"

            broadcast inv.Something
        }

        inv {
            name "2"

            tags(
                    my: 'tag'
            )

            require inv.Something
        }

        inv {
            name "3"

            when all tags(my: 'tag') completed {
                reached = true
            }
        }

        def report = executor.execute()
        assert report.isOk()
        assert reached
    }

    @Test
    void when_tag_not_ok() {

        boolean reached = false

        inv {
            name "1"

            tags(
                    my: 'tag'
            )
        }

        inv {
            name "2"

            when all tags(my: 'other-tag') completed {
                reached = true
            }
        }

        def report = executor.execute()
        assert !report.isOk()
        assert !reached
    }

    @Test
    void when_name_ok() {

        boolean reached = false

        inv {
            name "1"

            tags(
                    my: 'tag'
            )
        }

        inv {
            name "2"

            when all name "1" completed {
                reached = true
            }
        }

        def report = executor.execute()
        assert report.isOk()
        assert reached
    }

    @Test
    void when_name_ok_contains() {

        boolean reached = false

        inv {
            name "123"

            tags(
                    my: 'tag'
            )
        }

        inv {
            name "2"

            when all name "1" completed {
                reached = true
            }
        }

        def report = executor.execute()
        assert report.isOk()
        assert reached
    }

    @Test
    void when_name_not_ok() {

        boolean reached = false

        inv {
            name "1"

            tags(
                    my: 'tag'
            )
        }

        inv {
            name "2"

            when all name "2" completed {
                reached = true
            }
        }

        def report = executor.execute()
        assert !report.isOk()
        assert !reached
    }

    @Test
    void when_ok_broadcasts() {

        inv {
            name "1"

            tags(
                    my: 'tag'
            )
        }

        inv {
            name "2"

            when all tags(my: 'tag') completed {
                step {
                    broadcast inv.Something
                }
            }
        }

        inv {
            name "3"

            require inv.Something

            broadcast inv.Else
        }

        inv {
            name "4"

            require inv.Else
        }

        def report = executor.execute()
        assert report.isOk()
    }

    @Test
    void when_ok_require() {

        boolean raised = false

        inv {
            name "1"

            tags(
                    my: 'tag'
            )

            broadcast inv.Something
        }

        inv {
            name "2"

            tags(
                    my: 'tag'
            )

            require inv.Something

            when all tags(my: 'tag') completed {
                raised = true
            }
        }

        def report = executor.execute()
        assert report.isOk()
        assert raised
    }

    @Test
    void when_error_require() {

        boolean raised = false

        inv {
            name "1"

            tags(
                    my: 'tag'
            )

            broadcast inv.Something
        }

        inv {
            name "2"

            tags(
                    my: 'tag'
            )

            require inv.Else

            when all tags(my: 'tag') completed {
                raised = true
            }
        }

        def report = executor.execute()
        assert !report.isOk()
        assert !raised
    }

    @Test
    void multiple_steps() {

        def size = 10
        def steps = []
        1.upto(size, { final Number reference ->
            steps << {
                assert reference == it + 1
            }
        })

        inv {

            name "steps"

            for (Closure body : steps) {
                step(body)
            }
        }
        def report = executor.execute()
        assert report.isOk()
    }

    @Test
    void multiple_steps_require_and_broadcasts() {
        inv {
            name "step1"

            step {
                assert it == 0

                require inv.Something
            }

            step {
                assert it == 1

                broadcast inv.Else
                require inv.More
            }

            step {
                assert it == 2

                broadcast inv.Final
            }
        }

        def steps2 = [

        ]
        inv {
            name "step2"

            step {
                assert it == 0

                broadcast inv.Something
            }

            step {
                assert it == 1

                require inv.Else
                broadcast inv.More
            }

            step {
                assert it == 2

                require inv.Final
            }
        }

        def report = executor.execute()
        assert report.isOk()
    }

    @Test
    void ok_doc() {

        inv {
            name "doc"
            markdown '''
This is a sample description for this INV.
'''

            broadcast inv.Something using {
                markdown '''
This is a sample description for this  
broadcast statement
'''
            }

            require inv.Something using {
                markdown '''
This is a sample description for **this** require statement
'''
            }

            broadcast inv.Else using {
                markdown '''
This is a sample description for **this** broadcast statement
'''
            }
        }

        def report = executor.execute()
        assert report.isOk()
        assert new File(RunsRoller.latest.folder(), "report.md").exists()
    }
}