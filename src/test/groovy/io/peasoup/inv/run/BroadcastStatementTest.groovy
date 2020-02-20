package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

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

            broadcast inv.Element using {
                ready {[
                    my: { return "method" }
                ]}
            }
        }

        inv {
            name "consume"

            require inv.Element using {

                resolved {
                    assert my
                    assert my instanceof Closure
                    assert my() == "method"
                }
            }
        }

        executor.execute()
    }

    @Test
    void not_existing() {
        inv {
            name "provide"

            broadcast inv.Element using {
                ready { return null }
            }
        }

        inv {
            name "consume"

            require inv.Element using {

                resolved {
                    assert my == null
                    assert my() == null
                }
            }
        }

        executor.execute()
    }

    @Test
    void not_existing_2() {
        inv {
            name "provide"

            broadcast inv.Element using {
                ready {[
                    something: "valuable"
                ]}
            }
        }

        inv {
            name "consume"

            require inv.Element using {

                resolved {
                    assert my == null
                    assert my() == null
                }
            }
        }

        executor.execute()
    }

    @Test
    void not_a_closure() {
        inv {
            name "provide"

            broadcast inv.Element using {
                ready {[
                        my: "method"
                ]}
            }
        }

        inv {
            name "consume"

            require inv.Element using {

                resolved {
                    assert my
                    assert !(my instanceof Closure)
                    assert my() == null
                }
            }
        }

        executor.execute()
    }

    @Test
    void ok_with_property() {
        inv {
            name "provide"

            broadcast inv.Element using {
                ready {[
                    my: "property"
                ]}
            }
        }

        inv {
            name "consume"

            require inv.Element using {

                resolved {
                    assert my
                    assert my == "property"

                    println my
                }
            }
        }

        executor.execute()
    }

    @Test
    void ok_with_default() {
        inv {
            name "provide"

            broadcast inv.Element using {
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

            require inv.Element using {

                resolved {
                    assert my
                    assert my == "default-value"
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

        assert statement.state == StatementStatus.NOT_PROCESSED
    }

    @Test
    void already_broadcasted() {

        BroadcastStatement statement = new BroadcastStatement()
        statement.name = "Statement"
        statement.id = "my-id"

        NetworkValuablePool pool = new NetworkValuablePool()

        pool.availableStatements.put(statement.name, [
                (statement.id): new BroadcastResponse()
        ] as Map<Object, BroadcastResponse>)

        BroadcastStatement.BROADCAST.manage(pool, statement)
        assert statement.state == StatementStatus.ALREADY_BROADCAST

        pool.availableStatements[statement.name].clear()
        statement.state = StatementStatus.NOT_PROCESSED

        pool.stagingStatements.put(statement.name, [
                (statement.id): new BroadcastResponse()
        ] as Map<Object, BroadcastResponse>)

        BroadcastStatement.BROADCAST.manage(pool, statement)
        assert statement.state == StatementStatus.ALREADY_BROADCAST
    }
}
