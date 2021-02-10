package io.peasoup.inv.composer.run1

import io.peasoup.inv.Home

@groovy.transform.BaseScript(groovy.util.DelegatingScript.class)
import static org.junit.Assert.*

// Get links
Map links = links()

// Wait until booting
waitFor(10) {
    get(links.setup) { it.booted }
}

def runFileTxt = new File(Home.getCurrent(), "run.txt").text

// Get content
getAsString(links.runFile.default) {
    assertNotNull it
    assertEquals runFileTxt, it
}

// Update content
post(links.runFile.save, runFileTxt) {
    assertNotNull it
    assertEquals "Runfile updated", it.result
}