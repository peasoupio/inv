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

        executor.execute()
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

        executor.execute()
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

        executor.execute()
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

        executor.execute()
    }

    @Test
    void ok_with_property() {
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

        executor.execute()
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

        executor.execute()
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
                (statement.id): new BroadcastResponse("resolvedByMe", null,null)
        ] as Map<Object, BroadcastResponse>)

        BroadcastStatement.BROADCAST.manage(pool, statement)
        assertEquals StatementStatus.ALREADY_BROADCAST, statement.state

        pool.availableStatements[statement.name].clear()
        statement.state = StatementStatus.NOT_PROCESSED

        pool.stagingStatements.put(statement.name, [
                (statement.id): new BroadcastResponse("resolvedByMe", null,null)
        ] as Map<Object, BroadcastResponse>)

        BroadcastStatement.BROADCAST.manage(pool, statement)
        assertEquals StatementStatus.ALREADY_BROADCAST, statement.state
    }
}
