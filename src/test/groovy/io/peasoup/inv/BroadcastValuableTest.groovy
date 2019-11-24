package io.peasoup.inv

import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class BroadcastValuableTest {

    InvHandler inv

    @Before
    void setup() {
        inv = new InvHandler()
    }

    @Test
    void asdelegate_not_ok() {
        assertThrows(PowerAssertionError.class, {
            new BroadcastValuable.Response().asDelegate(null, false)
        })
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

        inv()
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

        inv()
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
            name "consume"

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
            name "consume"

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
