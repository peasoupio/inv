package io.peasoup.inv.composer.exec1

import io.peasoup.inv.Home

@groovy.transform.BaseScript(groovy.util.DelegatingScript.class)
import static org.junit.Assert.*

def runFile = new File(Home.getCurrent(), "run.txt")
runFile.delete() // Make sure run.txt is deleted

assertFalse runFile.exists()

// Get links
Map links = links()

// Wait until booting
waitFor(10) {
    get(links.setup) { it.booted }
}

// Stage "io.peasoup.public.net.http"
Map httpInv
get(links.repos.default) {
    assertNotNull it
    assertNotNull it.descriptors
    assertEquals 1, it.total

    httpInv = it.descriptors.find { it.name.equals("io.peasoup.inv.pub.net.http") }
    assertNotNull httpInv
}
post(httpInv.links.stage)

// Check if execution is running (it should not)
Map execLinks
get(links.execution.default) {
    assertNotNull it
    assertFalse it.running

    execLinks = it.links
}

// Start execution
post(execLinks.start, [debugMode: false, secureMode: false]) {
    assertNotNull it
    assertEquals 1, it.files.size()
}

//Wait a bit for it to actually start
Thread.sleep(1000)

// Check if execution is running (it should)
get(links.execution.default) {
    assertNotNull it
    assertTrue it.running
}

// Wait until execution is done
waitFor(30) {
    get(links.execution.default) { !it.running }
}

// Fetch execLinks again to get download link
get(links.execution.default) {
    assertNotNull it
    assertFalse it.running

    execLinks = it.links
}

// Download log
getAsString(execLinks.download) {
    assertNotNull it
}

// Check review
get(links.review.default) {
    assertNotNull it
    assertNotNull it.baseExecution
    assertNotNull it.lastExecution
    assertNotNull it.lines
    assertNotNull it.stats
}

// Promote
post(links.review.promote, null) {
    assertNotNull it
    assertEquals "promoted", it.result

    assertTrue runFile.exists()
}