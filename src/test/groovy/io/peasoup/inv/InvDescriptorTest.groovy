package io.peasoup.inv

import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class InvDescriptorTest {

    InvDescriptor inv

    @Before
    void setup() {
        ExpandoMetaClass.enableGlobally()
        inv = new InvDescriptor()
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


        def digested = inv()

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


        def digested = inv()

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
                resolved {
                    assert resolvedBy == "my-webservice-2"
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


        def digested = inv()

        assert digested.size() == 3
        assert digested[0].name == "my-server"
        assert digested[1].name == "my-webservice-2"
        assert digested[2].name == "my-app"
    }
}
