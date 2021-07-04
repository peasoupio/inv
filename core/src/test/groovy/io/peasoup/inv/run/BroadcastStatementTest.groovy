package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

class BroadcastStatementTest {

    InvExecutor executor
    InvHandler inv

    @Before
    void setup() {
        executor = new InvExecutor()
        inv = new InvHandler(executor)
    }

    @Test
    void during_halting() {

        NetworkValuablePool pool = new NetworkValuablePool()
        pool.startCleaning()
        pool.startHalting()

        BroadcastStatement statement = new BroadcastStatement()

        BroadcastStatement.BROADCAST_PROCESSOR.process(pool, statement)

        assertEquals StatementStatus.NOT_PROCESSED, statement.state
    }

    @Test
    void already_broadcasted() {

        NetworkValuablePool pool = new NetworkValuablePool()

        Inv inv = new Inv.Builder(pool).build()
        inv.name = "resolvedByMe"

        Inv otherInv = new Inv.Builder(pool).build()
        otherInv.name = "tryingToResolve"

        BroadcastStatement statement = new BroadcastStatement(inv: inv, name: "Statement", id: "my-id")

        def broadcast = new BroadcastStatement(inv: otherInv, name: statement.name, id: statement.id)
        def response = new BroadcastResponse(broadcast)
        pool.availableMap.staticIdStatements.put(broadcast, response)

        BroadcastStatement.BROADCAST_PROCESSOR.process(pool, statement)
        assertEquals StatementStatus.ALREADY_BROADCAST, statement.state

        pool.availableMap.staticIdStatements.clear()
        statement.state = StatementStatus.NOT_PROCESSED

        def broadcast2 = new BroadcastStatement(inv: otherInv, name: statement.name, id: statement.id)
        def response2 = new BroadcastResponse(broadcast2)
        pool.stagingMap.staticIdStatements.put(broadcast2, response2)

        BroadcastStatement.BROADCAST_PROCESSOR.process(pool, statement)
        assertEquals StatementStatus.ALREADY_BROADCAST, statement.state
    }

    @Test
    void not_ok() {
        assertThrows(IllegalArgumentException.class, {
            BroadcastStatement.BROADCAST_PROCESSOR.process(null, null)
        })

        assertThrows(IllegalArgumentException.class, {
            BroadcastStatement.BROADCAST_PROCESSOR.process(new NetworkValuablePool(), null)
        })
    }

    @Test
    void ok_resolvedBy() {
        inv {
            name "provide"

            broadcast { Element } using {
                global {[
                        my: { return "method" }
                ]}
            }
        }

        inv {
            name "consume"

            require { Element } using {

                resolved {
                    assertNotNull "provide", resolvedBy
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void ok_asBoolean() {
        inv {
            name "provide"

            broadcast { Element } using {
                global {[
                        my: { return "method" }
                ]}
            }
        }

        inv {
            name "consume"

            require { Element } using {

                resolved {
                    def valid = false
                    if (response) // raise asBoolean
                        valid = true

                    assertTrue  valid
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void ok_with_method() {
        inv {
            name "provide"

            broadcast { Element } using {
                global {[
                    my: { return "method" }
                ]}
            }
        }

        inv {
            name "consume"

            require { Element } using {

                resolved {
                    assertNotNull response.my
                    assertTrue response.my instanceof Closure
                    assertEquals "method", response.my()
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void response_not_existing() {
        inv {
            name "provide"

            broadcast { Element } using {
                global { return null }
            }
        }

        inv {
            name "consume"

            require { Element } using {

                resolved {
                    assertNull my
                    assertNull my()
                }
            }
        }

        def results = executor.execute()
        assertFalse results.report.isOk()
    }

    @Test
    void response_not_existing_2() {
        inv {
            name "provide"

            broadcast { Element } using {
                global {[
                    something: "valuable"
                ]}
            }
        }

        inv {
            name "consume"

            require { Element } using {

                resolved {
                    assertNull my
                    assertNull my()
                }
            }
        }

        def results = executor.execute()
        assertFalse results.report.isOk()
    }

    @Test
    void response_not_a_closure() {
        inv {
            name "provide"

            broadcast { Element } using {
                global {[
                        my: "method"
                ]}
            }
        }

        inv {
            name "consume"

            require { Element } using {

                resolved {
                    assertNotNull my
                    assertFalse my instanceof Closure
                    assertNull my()
                }
            }
        }

        def results = executor.execute()
        assertFalse results.report.isOk()
    }

    @Test
    void ok_get_property() {
        inv {
            name "provide"

            broadcast { Element } using {
                global {[
                    my: "property"
                ]}
            }
        }

        inv {
            name "consume"

            require { Element } using {

                resolved {
                    assertNotNull my
                    assertEquals "property", my
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void ok_set_property() {
        def newValue = "newValue"

        inv {
            name "provide"

            broadcast { Element } using {
                global {[
                        my: "property"
                ]}
            }
        }

        inv {
            name "consume"

            require { Element } using {

                resolved {
                    my = newValue
                    assertEquals newValue, my
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void ok_with_dynamic() {
        inv {
            name "provide"

            broadcast { Element } using {
                global {[
                        my: "global-value"
                ]}

                dynamic {[
                        my: "dynamic-value"
                ]}
            }
        }

        inv {
            name "consume"

            require { Element } using {
                resolved {
                    assertNotNull my
                    assertEquals "global-value", my
                }
            }
        }

        inv {
            name "consume-dynamic"

            require { Element } using {
                dynamic true
                resolved {
                    assertNotNull my
                    assertEquals "dynamic-value", my
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void ok_with_dynamic_2() {

        def id = [my: "id"]

        inv {
            name "provide"

            broadcast { Element } using {
                dynamic { it }
            }
        }

        inv {
            name "consume"

            require { Element(id) } using {
                dynamic true
                resolved {
                    assertNotNull my
                    assertEquals id.my, my
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void ok_with_dynamic_3() {

        def id = [my: "id"]
        def id2 = [my: "id2"]

        inv {
            name "provide"

            broadcast { Element } using {
                dynamic { it }
            }
        }

        inv {
            name "consume"

            require { Element(id) } using {
                dynamic true
                resolved {
                    assertEquals 1, executor.pool.availableMap.staticIdStatements.size()
                }
            }

            require { Element(id) } using {
                dynamic true
                resolved {
                    assertEquals 1, executor.pool.availableMap.staticIdStatements.size()
                }
            }
        }

        inv {
            name "consume2"

            require { Element(id2) } using {
                dynamic true
                resolved {
                    assertEquals 2, executor.pool.availableMap.staticIdStatements.size()
                }
            }

            require { Element(id2) } using {
                dynamic true
                resolved {
                    assertEquals 2, executor.pool.availableMap.staticIdStatements.size()
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void delegatedBroadcast_ok() {
        String myCtx = "/my-context"

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    dynamic {
                        [
                                withContext: { String ctx ->
                                    caller.broadcast { Endpoint(context: ctx) }
                                }
                        ]
                    }
                }
            }
        }

        inv {
            name "my-webservice"

            require { EndpointMapper } using {
                dynamic true
                resolved {
                    response.withContext(myCtx)
                }
            }
        }

        inv {
            name "my-other-app"

            require { Endpoint(context: myCtx) }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void delegatedBroadcast_withOtherBroadcast2() {
        String myCtx = "/my-context"
        String myNewCtx = "/my-new-context"

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    global { [my: "value"] }
                }
            }
        }

        inv {
            name "my-proxy"

            require { EndpointMapper } into '$mapper'

            broadcast { EndpointMapperProxy } using {
                global {
                    return $mapper
                }
            }
        }

        def results = executor.execute()
        assertFalse results.report.isOk()
    }

    @Test
    void is_delayed_false() {
        boolean reached = false

        inv {
            name "is_delayed_false"

            broadcast { Delayed } using {
                delayed false
                global { reached = true  }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
        assertTrue reached
    }

    @Test
    void is_delayed_true() {
        boolean reached = false

        inv {
            name "is_delayed_true"

            broadcast { Delayed } using {
                delayed true
                global { reached = true  }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
        assertFalse reached
    }

    @Test
    void is_delayed_true_require() {
        boolean reached = false

        inv {
            name "is_delayed_true"

            broadcast { Delayed } using {
                delayed true
                global { reached = true  }
            }

            require { Delayed }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
        assertTrue reached
    }

    @Test
    void is_delayed_once() {
        int reached = 0

        inv {
            name "is_delayed_once"

            broadcast { Delayed } using {
                delayed true
                global { reached++  }
            }
        }

        inv {
            name "is_delayed_once_consumer"

            require { Delayed }
            require { Delayed }
            require { Delayed }

        }

        def results = executor.execute()
        assertTrue results.report.isOk()
        assertEquals 1, reached
    }

}
