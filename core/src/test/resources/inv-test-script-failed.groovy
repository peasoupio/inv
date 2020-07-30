@Test
void ok() {
    simulate(
        "/testing/inv1-provider.groovy",
        "/testing/inv1.groovy"
    )

    assertEquals true

    assert isOk
}

@Test
void missing_element() {
    simulate(
        "/testing/inv1.groovy"
    )

    assert isHalted
}

