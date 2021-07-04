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
    void ClassWithCaller_set_property() {
        def newValue = "newValue"

        inv {
            name "provide"

            broadcast { Element } using {
                dynamic { new ClassWithCaller(myCtx: "myCtx") }
            }
        }

        inv {
            name "consume"

            require { Element } using {
                dynamic true
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
    void ClassWithCaller_has_property() {
        inv {
            name "provide"

            broadcast { Element } using {
                dynamic { new ClassWithCaller(myCtx: "myCtx") }
            }
        }

        inv {
            name "consume"

            require { Element } using {
                dynamic true
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
    void ClassWithCaller_as_global() {
        String myCtx = "/my-context"

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    global { return new ClassWithCaller(myCtx: myCtx) }
                }
            }
        }

        inv {
            name "my-webservice"

            require { EndpointMapper } using {
                resolved {
                    assertEquals myCtx, response.myCtx
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void ClassWithCaller_delegate_broadcast() {
        String myCtx = "/my-context"

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    dynamic { return new ClassWithCaller() }
                }
            }
        }

        inv {
            name "my-webservice"

            require { EndpointMapper } using {
                dynamic true
                resolved {
                    def resp = response as ClassWithCaller
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
    void ClassWithCaller_set_property_later() {
        String myCtx = "/my-context"
        String myNewCtx = "/my-new-context"

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    dynamic { return new ClassWithCaller(myCtx: myCtx) }
                }
            }
        }

        inv {
            name "my-webservice"

            require { EndpointMapper } using {
                dynamic true
                resolved {
                    def respCls2 = response as ClassWithCaller
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
                dynamic true
                resolved {
                    def respCls2 = response as ClassWithCaller
                    assertEquals myCtx, respCls2.myCtx
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void ClassWithCaller_rebroadcast_response() {

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    global { return new ClassWithCaller() }
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
    void ClassWithCaller_using_caller_info() {

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    dynamic { return new ClassWithCaller() }
                }
            }
        }

        String subPath = "subpath1"
        inv {
            name "my-webservice"
            path subPath

            require { EndpointMapper } using {
                dynamic true
                resolved {
                    def resp = response as ClassWithCaller
                    assertNotNull resp.myPath
                    assertEquals path, resp.myPath
                }
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void ClassWithCaller_as_interface() {

        inv {
            name "my-server-id"

            step {
                broadcast { EndpointMapper } using {
                    dynamic { return new ClassWithCaller(myCtx: "ok") }
                }
            }
        }

        inv {
            name "my-webservice"

            require { EndpointMapper } using {
                dynamic true
                resolved {
                    def respInterface = response as InterfaceForCaller
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
                    global { return new ClassWithCtor(context) }
                }
            }
        }

        inv {
            name "my-webservice"

            require { EndpointMapper } using {
                resolved {
                    assertNotNull response.context

                    //throws "MissingPropertyException"
                    response.notExistingProperty
                }
            }
        }

        def results = executor.execute()
        assertFalse results.report.isOk()
    }

    static class ClassWithCaller {
        String myCtx

        void withContext(String ctx) {
            getCaller().broadcast { Endpoint(context: ctx) }
        }

        void withContext2(InvDescriptor myself, String ctx) {
            myself.broadcast { Endpoint(context: ctx) }
        }

        String getMyPath() { getCaller().path }

        String interfacable() { return myCtx }
    }

    interface InterfaceForCaller {
        String interfacable()
    }

    static class ClassWithCtor {

        private String context

        ClassWithCtor(String context) {
            this.context = context
        }

    }
}
