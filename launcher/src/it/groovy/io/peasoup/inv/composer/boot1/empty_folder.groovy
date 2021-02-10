package io.peasoup.inv.composer.boot1

import static junit.framework.Assert.assertEquals
@groovy.transform.BaseScript(groovy.util.DelegatingScript.class)
import static org.junit.Assert.*

// Get links
Map links = links()

get(links.setup) {
    assertNotNull it
    assertEquals "my-version", it.releaseVersion
    assertTrue it.initInfo.standalone
    assertTrue it.firstTime
}