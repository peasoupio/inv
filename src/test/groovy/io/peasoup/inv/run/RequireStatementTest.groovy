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
    void ok_with_unbloating() {

        boolean unresolvedRaised = false

        inv {
            name "consume"

            require inv.Bloatable using {
                unbloatable true

                unresolved {
                    unresolvedRaised = true

                    assert it.name == "Bloatable"
                    assert it.id == InvDescriptor.DEFAULT_ID
                    assert it.owner == "consume"
                }
            }
        }

        executor.execute()

        assert unresolvedRaised
    }

    @Test
    void ok_with_halting() {

        boolean unresolvedRaised = false

        inv {
            name "consume"

            require inv.Bloatable using {
                unbloatable false

                unresolved {
                    unresolvedRaised = true
                }
            }
        }

        executor.execute()

        assert !unresolvedRaised
    }

    @Test
    void ok_with_into() {

        inv {
            name "provide"

            broadcast inv.Element
        }

        inv {
            name "consume"

            require inv.Element into '$element'

            step {
                assert $element
            }
        }

        executor.execute()
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
