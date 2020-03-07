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

    class MyResponseClass1 {
        Closure withContext(String ctx) {{
            broadcast inv.Endpoint(context: ctx)
        } as Closure}
    }

    class MyResponseClass2 {
        String myCtx

        Closure onDefault() {{
            return [defaultContext: myCtx]
        } as Closure}

        Closure withContext(String ctx) {{
            broadcast inv.Endpoint(context: ctx)
        } as Closure}
    }
}
