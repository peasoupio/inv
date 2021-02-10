package io.peasoup.inv.composer.system1

@groovy.transform.BaseScript(groovy.util.DelegatingScript.class)
import static org.junit.Assert.*
import static org.junit.Assert.assertThrows

// Get links
Map links = links()

// Get settings
get(links.settings.default) {
    assertNotNull it
}

// Get initfile
getAsString(links.initFile.default) {
    assertNotNull it
}

// Pull initfile
post(links.initFile.pull) {
    assertNotNull it
    assertEquals "Pulled init successfully", it.result
}

// Push initfile
post(links.initFile.push) {
    assertNotNull it
    assertEquals "Pushed init successfully", it.result
}