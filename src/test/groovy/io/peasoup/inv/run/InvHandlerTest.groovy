package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

import java.util.stream.Stream

import static org.junit.jupiter.api.Assertions.assertThrows

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

        report.errors.each {
            it.exception.printStackTrace()
        }

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

        Stream.of("A", "B","C", "D", "E")
            .parallel()
            .each { provider ->
                patternProvider(provider)

                Stream.of("A", "B","C", "D", "E")
                    .parallel()
                    .each {consumer ->
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
        assert report.errors.find { it.exception.message == "fail" }


        report.errors.each {
            println "=================="
            println "INV: ${it.inv.name}"
            it.exception.printStackTrace()
            println "=================="
        }
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
        assert report.errors.find { it.exception.message == "fail-broadcast" }
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
        assert report.errors.find { it.exception.message == "fail-require" }
    }
}
