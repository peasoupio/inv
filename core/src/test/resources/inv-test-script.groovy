@Test
void ok() {
    simulate {
        addInvFile "/testing/inv1-provider.groovy"
        addInvFile "/testing/inv1.groovy"
    }

    assertTrue true

    assert isOk
}

@Test
void missing_element() {
    simulate {
        addInvFile "/testing/inv1.groovy"
    }

    assert isHalted
}

