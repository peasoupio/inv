package io.peasoup.inv.testing

import org.junit.Test

class TestingExecutorTest extends JUnitInvTestingBase {

    @Test
    void ok() {
        sequence(
        "/testing/inv1-provider.groovy",
            "/testing/inv1.groovy"
        )

        assert isOk
    }

    @Test
    void missing_element() {
        sequence(
        "/testing/inv1.groovy"
        )

        assert isHalted
    }
}
