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
            name "conusme"

            require inv.Element using {

                resolved {
                    assert my
                    assert my instanceof Closure
                    assert my() == "method"
                }
            }
        }

        inv()
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
            name "conusme"

            require inv.Element using {

                resolved {
                    assert my
                    assert my == "property"

                    println my
                }
            }
        }

        inv()
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
