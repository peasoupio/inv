package io.peasoup.inv.main.repo.testsuccess.test

import org.junit.Test

@Test
void ok() {

    simulate(
        "/resources/test/inv1.groovy",
        "/resources/test/inv2.groovy"
    )

    assertTrue isOk
    assertFalse isHalted
}

@Test
void missing_element() {
    simulate(
        "/resources/test/inv2.groovy"
    )

    assertFalse isOk
    assertTrue isHalted
}

