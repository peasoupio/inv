package io.peasoup.inv.composer.boot1

import static junit.framework.Assert.assertEquals
@groovy.transform.BaseScript(groovy.util.DelegatingScript.class)
import static junit.framework.TestCase.*

String setupAPI

get("/api/v1") {
    assertNotNull it
    assertNotNull it.links

    setupAPI = it.links.setup
}

get(setupAPI) {
    assertNotNull it
    assertEquals "my-version", it.releaseVersion
    assertTrue it.initInfo.standalone
    assertTrue it.firstTime
}