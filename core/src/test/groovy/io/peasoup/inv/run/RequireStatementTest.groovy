package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

class RequireStatementTest {

    InvExecutor executor
    InvHandler inv

    @Before
    void setup() {
        executor = new InvExecutor()
        inv = new InvHandler(executor)
    }

    @Test
    void ok() {

        inv {
            name "provider"
            broadcast { Element }
        }

        inv {
            name "consumer"
            require { Element }
        }

        def results = executor.execute()

        assertTrue results.report.isOk()

        def requireStatement = executor
                .pool
                .totalInvs
                .find { it.name == "consumer" }
                .totalStatements
                .first() as RequireStatement

        assertNotNull requireStatement
        assertEquals StatementStatus.SUCCESSFUL, requireStatement.state
        assertFalse requireStatement.unbloatable
        assertTrue requireStatement.toString().contains("[REQUIRE]")
    }

    @Test
    void ok_with_unbloating() {

        boolean unresolvedRaised = false

        inv {
            name "consumer"

            require { Bloatable } using {
                unbloatable true

                unresolved {
                    unresolvedRaised = true

                    assertEquals "Bloatable", it.name
                    assertEquals StatementDescriptor.DEFAULT_ID, it.id
                    assertEquals "consumer", it.owner
                }
            }
        }

        def results = executor.execute()

        assertTrue results.report.isOk()
        assertTrue unresolvedRaised

        def requireStatement = executor.pool.totalInvs.first().totalStatements.first() as RequireStatement

        assertNotNull requireStatement
        assertTrue requireStatement.unbloatable
        assertTrue requireStatement.toString().contains("[UNBLOATABLE]")
    }

    @Test
    void ok_with_halting() {

        boolean unresolvedRaised = false

        inv {
            name "consumer"

            require { Bloatable } using {
                unbloatable false

                unresolved {
                    unresolvedRaised = true
                }
            }
        }

        def results = executor.execute()

        assertFalse results.report.isOk()
        assertFalse unresolvedRaised
    }

    @Test
    void ok_with_into() {

        inv {
            name "provider"

            broadcast { Element }
        }

        inv {
            name "consumer"

            require { Element } into '$element'

            step {
                assertNotNull $element
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void ok_with_default_into() {

        inv {
            name "provider"

            broadcast { Element }
        }

        inv {
            name "consumer"

            require { Element }

            step {
                assertNotNull $element
            }
        }

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    @Test
    void during_halting() {

        NetworkValuablePool pool = new NetworkValuablePool()
        pool.startUnbloating()
        pool.startHalting()

        RequireStatement statement = new RequireStatement()

        RequireStatement.REQUIRE.manage(pool, statement)

        assertEquals StatementStatus.NOT_PROCESSED, statement.state
    }

    @Test
    void not_ok() {
        assertThrows(IllegalArgumentException.class, {
            RequireStatement.REQUIRE.manage(null, null)
        })

        assertThrows(IllegalArgumentException.class, {
            RequireStatement.REQUIRE.manage(new NetworkValuablePool(), null)
        })
    }
}
