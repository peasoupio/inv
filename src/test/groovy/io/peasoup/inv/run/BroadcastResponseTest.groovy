package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

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
                broadcast inv.EndpointMapper using {
                    ready {
                        [
                                withContext: { String ctx ->
                                    broadcast inv.Endpoint(context: ctx)
                                }
                        ]
                    }
                }
            }
        }

        inv {
            name "my-webservice"

            require inv.EndpointMapper using {
                resolved {
                    response.withContext(myCtx)
                }
            }
        }

        inv {
            name "my-other-app"

            require inv.Endpoint(context: myCtx)
        }

        def report = executor.execute()
        assert report.isOk()
    }

    @Test
    void delegatedBroadcast_with_default_property() {
        String myCtx = "/my-context"

        inv {
            name "my-server-id"

            step {
                broadcast inv.EndpointMapper using {
                    ready {
                        [
                                $          : { [defaultContext: myCtx] },
                                withContext: { String ctx ->
                                    broadcast inv.Endpoint(context: ctx)
                                }
                        ]
                    }
                }
            }
        }

        inv {
            name "my-webservice"

            require inv.EndpointMapper using {
                resolved {
                    response.withContext(response.defaultContext)
                }
            }
        }

        inv {
            name "my-other-app"

            require inv.Endpoint(context: myCtx)
        }

        def report = executor.execute()
        assert report.isOk()
    }


    @Test
    void delegatedBroadcast_ok_withClass1() {
        String myCtx = "/my-context"

        inv {
            name "my-server-id"

            step {
                broadcast inv.EndpointMapper using {
                    ready {return new MyResponseClass1()}
                }
            }
        }

        inv {
            name "my-webservice"

            require inv.EndpointMapper using {
                resolved {
                    response.withContext(myCtx)
                }
            }
        }

        inv {
            name "my-other-app"

            require inv.Endpoint(context: myCtx)
        }

        def report = executor.execute()
        assert report.isOk()
    }

    @Test
    void delegatedBroadcast_ok_withClass2_with_myself() {
        String myCtx = "/my-context"

        inv {
            name "my-server-id"

            step {
                broadcast inv.EndpointMapper using {
                    ready {return new MyResponseClass2()}
                }
            }
        }

        inv {
            name "my-webservice"

            require inv.EndpointMapper using {
                resolved {
                    def resp = response as MyResponseClass2
                    resp.withContext2(myself, myCtx)
                }
            }
        }

        inv {
            name "my-other-app"

            require inv.Endpoint(context: myCtx)
        }

        def report = executor.execute()
        assert report.isOk()
    }

    @Test
    void delegatedBroadcast_with_default_property_withClass() {
        String myCtx = "/my-context"

        inv {
            name "my-server-id"

            step {
                broadcast inv.EndpointMapper using {
                    ready { return new MyResponseClass2(myCtx: myCtx) }
                }
            }
        }

        inv {
            name "my-webservice"

            require inv.EndpointMapper using {
                resolved {
                    response.withContext(response.defaultContext)
                }
            }
        }

        inv {
            name "my-other-app"

            require inv.Endpoint(context: myCtx)
        }

        def report = executor.execute()
        assert report.isOk()
    }

    @Test
    void delegatedBroadcast_with_set_property_withClass() {
        String myCtx = "/my-context"
        String myNewCtx = "/my-new-context"

        inv {
            name "my-server-id"

            step {
                broadcast inv.EndpointMapper using {
                    ready { return new MyResponseClass2(myCtx: myCtx) }
                }
            }
        }

        inv {
            name "my-webservice"

            require inv.EndpointMapper using {
                resolved {
                    def respCls2 = response as MyResponseClass2
                    assert respCls2.myCtx == myCtx

                    respCls2.myCtx = myNewCtx

                    response.withContext(response.myCtx)
                }
            }
        }

        inv {
            name "my-other-app"

            require inv.Endpoint(context: myNewCtx)

            require inv.EndpointMapper using {
                resolved {
                    def respCls2 = response as MyResponseClass2
                    assert respCls2.myCtx == myNewCtx
                }
            }
        }

        def report = executor.execute()
        assert report.isOk()
    }

    @Test
    void delegatedBroadcast_with_other_broadcast() {
        String myCtx = "/my-context"
        String myNewCtx = "/my-new-context"

        inv {
            name "my-server-id"

            step {
                broadcast inv.EndpointMapper using {
                    ready { return new MyResponseClass2(myCtx: myCtx) }
                }
            }
        }

        inv {
            name "my-proxy"

            require inv.EndpointMapper into '$mapper'

            broadcast inv.EndpointMapperProxy using {
                ready {
                    $mapper.defaultContext = myNewCtx

                    return $mapper
                }
            }
        }

        inv {
            name "my-other-app"

            require inv.EndpointMapperProxy using {
                resolved {
                    assert response.defaultContext == myNewCtx
                }
            }
        }

        def report = executor.execute()
        assert report.isOk()
    }

    class MyResponseClass1 {
        Closure withContext(String ctx) {{
            broadcast inv.Endpoint(context: ctx)
        } as Closure}
    }

    class MyResponseClass2 {
        String myCtx

        Closure onDefault() {
            {
                return [defaultContext: myCtx]
            } as Closure
        }

        Closure withContext(String ctx) {
            return {
                broadcast inv.Endpoint(context: ctx)
            } as Closure
        }

        void withContext2(InvDescriptor myself, String ctx) {
            myself.broadcast(myself.inv.Endpoint(context: ctx))
        }
    }
}
