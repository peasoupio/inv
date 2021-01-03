package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class BroadcastResponseTest {

    InvExecutor executor
    InvHandler inv

    @Before
    void setup() {
        executor = new InvExecutor()
        inv = new InvHandler(executor)
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

        def report = executor.execute()
        assertTrue report.isOk()
    }

    @Test
    void delegatedBroadcast_with_default_property() {
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

        def report = executor.execute()
        assertTrue report.isOk()
    }


    @Test
    void delegatedBroadcast_ok_withClass1() {
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

        def report = executor.execute()
        assertTrue report.isOk()
    }

    @Test
    void delegatedBroadcast_ok_withClass2_with_myself() {
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

        def report = executor.execute()
        assertTrue report.isOk()
    }

    @Test
    void delegatedBroadcast_with_default_property_withClass() {
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

        def report = executor.execute()
        assertTrue report.isOk()
    }

    @Test
    void delegatedBroadcast_with_set_property_withClass() {
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

        def report = executor.execute()
        assertTrue report.isOk()
    }

    @Test
    void delegatedBroadcast_with_other_broadcast() {
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

        def report = executor.execute()
        assertFalse report.isOk()
    }

    @Test
    void delegatedBroadcast_with_other_broadcast_2() {
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

        def report = executor.execute()
        assertFalse report.isOk()
    }

    class MyResponseClass1 {
        void withContext(String ctx) {
            broadcast { Endpoint(context: ctx) }
        }
    }

    class MyResponseClass2 {
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
}
