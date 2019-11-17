package io.peasoup.inv

import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class RequireValuableTest  {

    InvHandler inv

    @Before
    void setup() {
        ExpandoMetaClass.enableGlobally()
        inv = new InvHandler()
    }

    @Test
    void ok_with_unbloating() {

        boolean unresolvedRaised = false

        inv {
            name "consume"

            require inv.Bloatable using {
                unbloatable false

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
