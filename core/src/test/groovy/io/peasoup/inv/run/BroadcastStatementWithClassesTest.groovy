package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

class BroadcastStatementWithClassesTest {

    InvExecutor executor
    InvHandler inv

    @Before
    void setup() {
        executor = new InvExecutor()
        inv = new InvHandler(executor)
    }

    @Test
    void ClassWithDefault_set_property() {
        def newValue = "newValue"

        inv {
            name "provide"

            broadcast { Element } using {
                ready { new ClassWithDefault(myCtx: "myCtx") }
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
    void ClassWithDefault_has_property() {
        inv {
            name "provide"

            broadcast { Element } using {
                ready { new ClassWithDefault(myCtx: "myCtx") }
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
    void ClassWithDefault_set_property_on_default_value() {
        def newValue = "newValue"

        inv {
            name "provide"

            broadcast { Element } using {
                ready { new ClassWithDefault() }
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
    void ClassWithDefault_use_default_value() {
        String myCtx = "/my-context"

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    ready { return new ClassWithDefault(myCtx: myCtx) }
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
    void ClassWithDefault_delegate_broadcast() {
        String myCtx = "/my-context"

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    ready { return new ClassWithDefault() }
                }
            }
        }

        inv {
            name "my-webservice"

            require { EndpointMapper } using {
                resolved {
                    def resp = response as ClassWithDefault
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
    void ClassWithDefault_set_property_later() {
        String myCtx = "/my-context"
        String myNewCtx = "/my-new-context"

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    ready { return new ClassWithDefault(myCtx: myCtx) }
                }
            }
        }

        inv {
            name "my-webservice"

            require { EndpointMapper } using {
                resolved {
                    def respCls2 = response as ClassWithDefault
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
                    def respCls2 = response as ClassWithDefault
                    assertEquals myNewCtx, respCls2.myCtx
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void ClassWithDefault_rebroadcast_response() {
        String myCtx = "/my-context"
        String myNewCtx = "/my-new-context"

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    ready { return new ClassWithDefault(myCtx: myCtx) }
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
    void ClassWithDefaultUsingCallee_using_callee() {

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    ready { return new ClassWithDefaultUsingCallee() }
                }
            }
        }

        String subPath = "subpath1"
        inv {
            name "my-webservice"
            path subPath

            require { EndpointMapper } using {
                resolved {
                    def resp = response as ClassWithDefaultUsingCallee
                    assertNotNull resp.myPath
                    assertEquals path, resp.myPath
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void ClassWithDefaultUsingCallee_using_interface() {

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    ready { return new ClassWithDefaultUsingCallee() }
                }
            }
        }

        String subPath = "subpath1"
        inv {
            name "my-webservice"
            path subPath

            require { EndpointMapper } using {
                resolved {
                    def respInterface = response as ClassWithDefaultUsingCalleeInterface
                    assertEquals "ok", respInterface.interfacable()
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void ClassWithCtor_missing_property() {
        String context = "my-context"
        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    ready { return new ClassWithCtor(context) }
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



    static class ClassWithDefault {
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

    class ClassWithDefaultUsingCallee {
        Map $default() {
            String callerPath = getPath()
            return [myPath: callerPath]
        }

        String interfacable() { return "ok" }
    }

    interface ClassWithDefaultUsingCalleeInterface {
        String interfacable()
    }

    static class ClassWithCtor {
        private final String context

        ClassWithCtor(String context) {
            this.context = context
        }

        String $default() {
            [ctorContext: context]
        }
    }
}
