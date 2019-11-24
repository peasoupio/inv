package io.peasoup.inv

import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Before
import org.junit.Test

import java.util.stream.Stream

import static org.junit.jupiter.api.Assertions.assertThrows

class InvHandlerTest {

    InvHandler inv

    @Before
    void setup() {
        Logger.DebugModeEnabled = true

        inv = new InvHandler()
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


        def (List digested, Boolean success) = inv()
        assert success

        assert digested.size() == 3
        assert digested[0].name == "my-server"
        assert digested[1].name == "my-webservice"
        assert digested[2].name == "my-app"
    }

    @Test
    void call_not_ok() {

        assertThrows(PowerAssertionError.class, {
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
            name "my-app"

            require inv.Endpoint("my-webservice-id-not-existing")

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


        def (List digested, Boolean success) = inv()
        assert !success

        assert digested.size() == 2
        assert digested[0].name == "my-server"
        assert digested[1].name == "my-webservice"
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


        def (List digested, Boolean success) = inv()
        assert success

        digested
            .findAll { it.name.contains("my-webservice") }
            .collect { it.totalValuables }
            .any {
                it.state == NetworkValuable.ALREADY_BROADCAST
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

        def (List invs, Boolean success) = inv()
        assert success
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

        def (List invs, Boolean success) = inv()
        assert success
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

            require inv.App("my-app-id")
        }

        def (List invs, Boolean success) = inv()
        assert success
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

        def (List invs, Boolean success) = inv()
        assert success
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
            .forEach { provider ->
                patternProvider(provider)

                Stream.of("A", "B","C", "D", "E")
                    .parallel()
                    .forEach {consumer ->
                        patternConsumer(provider, provider + consumer)
                    }
            }

        def (List invs, Boolean success) = inv()
        assert success
    }
}
