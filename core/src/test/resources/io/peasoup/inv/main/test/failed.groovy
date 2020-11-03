@Test
void ok() {
    simulate(
        "/testing/inv1.groovy",
        "/testing/inv2.groovy"
    )

    assertTrue isOk
    assertFalse isHalted
}

@Test
void missing_element() {
    simulate(
        "/testing/inv2.groovy"
    )

    assertTrue isOk
    assertFalse isHalted
}

