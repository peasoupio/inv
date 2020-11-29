package io.peasoup.inv.composer.exec2

import io.peasoup.inv.Home

import static junit.framework.Assert.assertEquals
import static junit.framework.Assert.assertFalse
import static junit.framework.Assert.assertTrue
@groovy.transform.BaseScript(groovy.util.DelegatingScript.class)
import static junit.framework.TestCase.*
import static junit.framework.TestCase.assertEquals
import static junit.framework.TestCase.assertTrue

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

    httpInv = it.descriptors.find { it.name.equals("io.peasoup.public.net.http") }
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
post(execLinks.start)

//Wait a bit for it to actually start
Thread.sleep(1000)

// Check if execution is running (it should)
get(links.execution.default) {
    assertNotNull it
    assertTrue it.running
}

// Start execution
post(execLinks.stop)

//Wait a bit for it to actually stop
Thread.sleep(1000)

// Check if execution is running (it should)
get(links.execution.default) {
    assertNotNull it
    assertFalse it.running
}
