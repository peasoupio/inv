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
            name "provide"
            broadcast $inv.Element
        }

        inv {
            name "consume"
            require $inv.Element
        }

        def report = executor.execute()

        assertTrue report.isOk()

        def requireStatement = executor
                .pool
                .totalInvs
                .find { it.name == "consume" }
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
            name "consume"

            require $inv.Bloatable using {
                unbloatable true

                unresolved {
                    unresolvedRaised = true

                    assertEquals "Bloatable", it.name
                    assertEquals InvDescriptor.DEFAULT_ID, it.id
                    assertEquals "consume", it.owner
                }
            }
        }

        def report = executor.execute()

        assertTrue report.isOk()
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
            name "consume"

            require $inv.Bloatable using {
                unbloatable false

                unresolved {
                    unresolvedRaised = true
                }
            }
        }

        def report = executor.execute()

        assertFalse report.isOk()
        assertFalse unresolvedRaised
    }

    @Test
    void ok_with_into() {

        inv {
            name "provide"

            broadcast $inv.Element
        }

        inv {
            name "consume"

            require $inv.Element into '$element'

            step {
                assertNotNull $element
            }
        }

        def report = executor.execute()
        assertTrue report.isOk()
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
}
