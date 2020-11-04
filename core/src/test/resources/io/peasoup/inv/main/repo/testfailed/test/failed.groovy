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

    assertTrue isOk
    assertFalse isHalted
}

