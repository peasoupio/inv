@groovy.transform.BaseScript(io.peasoup.inv.testing.JunitScriptBase.class)
package io.peasoup.inv.main.repo.testsuccess.test

import org.junit.Test

@Test
void ok() {

    simulate {
        addInvFile "/resources/test/inv1.groovy"
        addInvFile "/resources/test/inv2.groovy"
    }

    assertTrue isOk
    assertFalse isHalted
}

@Test
void missing_element() {
    simulate {
        addInvFile "/resources/test/inv2.groovy"
    }

    assertFalse isOk
    assertTrue isHalted
}

