package io.peasoup.inv

import org.junit.Before
import org.junit.Test

class BroadcastValuableTest {

    InvHandler inv

    @Before
    void setup() {
        ExpandoMetaClass.enableGlobally()
        inv = new InvHandler()
    }

    @Test
    void ok() {

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
            name "conusme"

            require inv.Element using {

                resolved {
                    assert my
                    assert my == "default-value"
                }
            }
        }

        inv()
    }
}
