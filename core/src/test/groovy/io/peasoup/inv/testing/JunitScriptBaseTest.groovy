package io.peasoup.inv.testing

import io.peasoup.inv.TempHome
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(TempHome.class)
class JunitScriptBaseTest {


    @Test
    void ok() {
        def script = new JunitScript()
        script.setup()

        assertFalse script.isOk
        assertFalse script.isHalted
        assertFalse script.hasExceptions

        script.simulate {
            addInvBody {
                name "my-inv"
            }
        }

        assertTrue script.isOk
        assertFalse script.isHalted
        assertFalse script.hasExceptions
    }

    @Test
    void not_ok() {
        assertThrows(IllegalArgumentException.class, {
            new JunitScript().simulate(null)
        })

        // cannot simulate twice per test
        assertThrows(IllegalAccessException.class, {
            def script = new JunitScript()
            script.setup()

            script.simulate {
                addInvBody {
                    name 'my-inv'
                }
            }
            script.simulate {
                addInvBody {
                    name 'another-inv'
                }
            }
        })
    }


    private class JunitScript extends JunitScriptBase {

        @Override
        Object run() {
            // does nothing
        }
    }
}
