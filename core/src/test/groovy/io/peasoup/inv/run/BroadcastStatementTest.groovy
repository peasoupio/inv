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
        pool.startUnbloating()
        pool.startHalting()

        BroadcastStatement statement = new BroadcastStatement()

        BroadcastStatement.BROADCAST.manage(pool, statement)

        assertEquals StatementStatus.NOT_PROCESSED, statement.state
    }

    @Test
    void already_broadcasted() {

        BroadcastStatement statement = new BroadcastStatement()
        statement.name = "Statement"
        statement.id = "my-id"

        NetworkValuablePool pool = new NetworkValuablePool()

        pool.availableStatements.put(statement.name, [
                (statement.id): new BroadcastResponse("resolvedByMe", null)
        ] as Map<Object, BroadcastResponse>)

        BroadcastStatement.BROADCAST.manage(pool, statement)
        assertEquals StatementStatus.ALREADY_BROADCAST, statement.state

        pool.availableStatements[statement.name].clear()
        statement.state = StatementStatus.NOT_PROCESSED

        pool.stagingStatements.put(statement.name, [
                (statement.id): new BroadcastResponse("resolvedByMe", null)
        ] as Map<Object, BroadcastResponse>)

        BroadcastStatement.BROADCAST.manage(pool, statement)
        assertEquals StatementStatus.ALREADY_BROADCAST, statement.state
    }

    @Test
    void not_ok() {
        assertThrows(IllegalArgumentException.class, {
            BroadcastStatement.BROADCAST.manage(null, null)
        })

        assertThrows(IllegalArgumentException.class, {
            BroadcastStatement.BROADCAST.manage(new NetworkValuablePool(), null)
        })
    }

    @Test
    void ok_resolvedBy() {
        inv {
            name "provide"

            broadcast { Element } using {
                ready {[
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
                ready {[
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
                ready {[
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
    void not_existing() {
        inv {
            name "provide"

            broadcast { Element } using {
                ready { return null }
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
    void not_existing_2() {
        inv {
            name "provide"

            broadcast { Element } using {
                ready {[
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
    void not_a_closure() {
        inv {
            name "provide"

            broadcast { Element } using {
                ready {[
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
                ready {[
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
                ready {[
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
    void ok_set_property_withClass() {
        def newValue = "newValue"

        inv {
            name "provide"

            broadcast { Element } using {
                ready {new MyResponseClass2(myCtx: "myCtx")}
            }
        }

        inv {
            name "consume"

            require { Element } using {

                resolved {
                    myCtx = newValue
                    assertEquals newValue, myCtx
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void ok_has_property() {
        inv {
            name "provide"

            broadcast { Element } using {
                ready {new MyResponseClass2(myCtx: "myCtx")}
            }
        }

        inv {
            name "consume"

            require { Element } using {

                resolved {
                    assertNotNull response.hasProperty("myCtx")
                    assertNull response.hasProperty("notExisting")
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void ok_set_property_withDefault() {
        def newValue = "newValue"

        inv {
            name "provide"

            broadcast { Element } using {
                ready {new MyResponseClass2()}
            }
        }

        inv {
            name "consume"

            require { Element } using {

                resolved {
                    defaultContext = newValue
                    assertEquals newValue, defaultContext
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void ok_with_default() {
        inv {
            name "provide"

            broadcast { Element } using {
                ready {[
                        $: {
                            return [
                                my: "default-value"
                            ]
                        }
                ]}
            }
        }

        inv {
            name "consume"

            require { Element } using {

                resolved {
                    assertNotNull my
                    assertEquals "default-value", my
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
                    ready {
                        [
                                withContext: { String ctx ->
                                    broadcast { Endpoint(context: ctx) }
                                }
                        ]
                    }
                }
            }
        }

        inv {
            name "my-webservice"

            require { EndpointMapper } using {
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
    void delegatedBroadcast_withDefaultProperty() {
        String myCtx = "/my-context"

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    ready {
                        [
                                $          : { [defaultContext: myCtx] },
                                withContext: { String ctx ->
                                    broadcast { Endpoint(context: ctx) }
                                }
                        ]
                    }
                }
            }
        }

        inv {
            name "my-webservice"

            require { EndpointMapper } using {
                resolved {
                    response.withContext(response.defaultContext)
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
    void delegatedBroadcast_okWithClass1() {
        String myCtx = "/my-context"

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    ready { return new MyResponseClass1() }
                }
            }
        }

        inv {
            name "my-webservice"

            require { EndpointMapper } using {
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
    void delegatedBroadcast_okWithClass2() {
        String myCtx = "/my-context"

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    ready { return new MyResponseClass2() }
                }
            }
        }

        inv {
            name "my-webservice"

            require { EndpointMapper } using {
                resolved {
                    def resp = response as MyResponseClass2
                    resp.withContext2(myself, myCtx)
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
    void delegatedBroadcast_okWithClass3() {

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    ready { return new MyResponseClass3() }
                }
            }
        }

        String subPath = "subpath1"
        inv {
            name "my-webservice"
            path subPath

            require { EndpointMapper } using {
                resolved {
                    def resp = response as MyResponseClass3
                    assertNotNull resp.myPath
                    assertEquals path, resp.myPath

                    def respInterface = response as MyResponseClass3Interface
                    assertEquals "ok", respInterface.interfacable()
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void delegatedBroadcast_okWithClass4() {
        String context = "my-context"
        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    ready { return new MyResponseClass4(context) }
                }
            }
        }

        inv {
            name "my-webservice"

            require { EndpointMapper } using {
                resolved {
                    //throws "MissingPropertyException"
                    response.ctorContext
                }
            }
        }

        def results = executor.execute()
        assertFalse results.report.isOk()
    }

    @Test
    void delegatedBroadcast_withDefaultPropertyWithClass() {
        String myCtx = "/my-context"

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    ready { return new MyResponseClass2(myCtx: myCtx) }
                }
            }
        }

        inv {
            name "my-webservice"

            require { EndpointMapper } using {
                resolved {
                    response.withContext(response.defaultContext)
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
    void delegatedBroadcast_withSetPropertyWithClass() {
        String myCtx = "/my-context"
        String myNewCtx = "/my-new-context"

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    ready { return new MyResponseClass2(myCtx: myCtx) }
                }
            }
        }

        inv {
            name "my-webservice"

            require { EndpointMapper } using {
                resolved {
                    def respCls2 = response as MyResponseClass2
                    assertEquals myCtx, respCls2.myCtx

                    respCls2.myCtx = myNewCtx

                    respCls2.withContext(respCls2.myCtx)
                }
            }
        }

        inv {
            name "my-other-app"

            require { Endpoint(context: myNewCtx) }

            require { EndpointMapper } using {
                resolved {
                    def respCls2 = response as MyResponseClass2
                    assertEquals myNewCtx, respCls2.myCtx
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void delegatedBroadcast_withOtherBroadcast() {
        String myCtx = "/my-context"
        String myNewCtx = "/my-new-context"

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    ready { return new MyResponseClass2(myCtx: myCtx) }
                }
            }
        }

        inv {
            name "my-proxy"

            require { EndpointMapper } into '$mapper'

            broadcast { EndpointMapperProxy } using {
                ready {
                    return $mapper
                }
            }
        }

        def results = executor.execute()
        assertFalse results.report.isOk()
    }

    @Test
    void delegatedBroadcast_withOtherBroadcast2() {
        String myCtx = "/my-context"
        String myNewCtx = "/my-new-context"

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    ready { [my: "value"] }
                }
            }
        }

        inv {
            name "my-proxy"

            require { EndpointMapper } into '$mapper'

            broadcast { EndpointMapperProxy } using {
                ready {
                    return $mapper
                }
            }
        }

        def results = executor.execute()
        assertFalse results.report.isOk()
    }

    static class MyResponseClass1 {
        void withContext(String ctx) {
            broadcast { Endpoint(context: ctx) }
        }
    }

    static class MyResponseClass2 {
        String myCtx

        Map $default() {
            return [defaultContext: myCtx]
        }

        void withContext(String ctx) {
            broadcast { Endpoint(context: ctx) }
        }

        void withContext2(InvDescriptor myself, String ctx) {
            myself.broadcast { Endpoint(context: ctx) }
        }
    }

    class MyResponseClass3 {
        Map $default() {
            String callerPath = getPath()
            return [myPath: callerPath]
        }

        String interfacable() { return "ok" }
    }

    interface MyResponseClass3Interface {
        String interfacable()
    }

    static class MyResponseClass4 {
        private final String context

        MyResponseClass4(String context) {
            this.context = context
        }

        String $default() {
            [ctorContext: context]
        }
    }
}
