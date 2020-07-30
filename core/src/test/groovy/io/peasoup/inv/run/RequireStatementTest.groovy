package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

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

        assert report.isOk()

        def requireStatement = executor
                .pool
                .totalInvs
                .find { it.name == "consume" }
                .totalStatements
                .first() as RequireStatement

        assert requireStatement
        assert requireStatement.state == StatementStatus.SUCCESSFUL
        assert !requireStatement.unbloatable
        assert requireStatement.toString().contains("[REQUIRE]")
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

                    assert it.name == "Bloatable"
                    assert it.id == InvDescriptor.DEFAULT_ID
                    assert it.owner == "consume"
                }
            }
        }

        def report = executor.execute()

        assert report.isOk()
        assert unresolvedRaised

        def requireStatement = executor.pool.totalInvs.first().totalStatements.first() as RequireStatement

        assert requireStatement
        assert requireStatement.unbloatable
        assert requireStatement.toString().contains("[UNBLOATABLE]")

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

        assert !report.isOk()
        assert !unresolvedRaised
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
                assert $element
            }
        }

        def report = executor.execute()

        assert report.isOk()
    }

    @Test
    void during_halting() {

        NetworkValuablePool pool = new NetworkValuablePool()
        pool.startUnbloating()
        pool.startHalting()

        RequireStatement statement = new RequireStatement()

        RequireStatement.REQUIRE.manage(pool, statement)

        assert statement.state == StatementStatus.NOT_PROCESSED
    }
}
