package io.peasoup.inv


import org.junit.Before
import org.junit.Test

class RequireValuableTest  {

    InvHandler inv

    @Before
    void setup() {
        inv = new InvHandler()
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
                    assert it.id == NetworkValuable.DEFAULT_ID
                    assert it.owner == "consume"
                }
            }
        }

        inv()

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

        inv()

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

        inv()
    }
}
