package io.peasoup.inv.composer.secure1

@groovy.transform.BaseScript(groovy.util.DelegatingScript.class)
import static org.junit.Assert.*

// Get links
Map _links = links()

// Wait until booting
waitFor(10) {
    get(_links.setup) { assertTrue it.booted }
}

// Check if links are not available
assertNull _links.stop; post("/api/stop", { assertEquals "Not available", it.message })

assertNull _links.settings
get("/api/settings", {
    assertEquals "Not available", it.message
})
post("/api/settings", {
    assertEquals "Not available", it.message
})

assertNull _links.run.stageAll; post("/api/run/stageAll", { assertEquals "Not available", it.message })
assertNull _links.run.unstageAll; post("/api/run/unstageAll", { assertEquals "Not available", it.message })

assertNull _links.repos.add; post("/api/repos/add", { assertEquals "Not available", it.message })
assertNull _links.repos.stageAll; post("/api/repos/stageAll", { assertEquals "Not available", it.message })
assertNull _links.repos.unstageAll; post("/api/repos/unstageAll", { assertEquals "Not available", it.message })
assertNull _links.repos.applyDefaultAll; post("/api/repos/applyDefaultAll", { assertEquals "Not available", it.message })
assertNull _links.repos.resetAll; post("/api/repos/resetAll", { assertEquals "Not available", it.message })

assertNull _links.initFile.save; post("/api/initfile", { assertEquals "Not available", it.message })
assertNull _links.initFile.pull; post("/api/initfile/pull", { assertEquals "Not available", it.message })
assertNull _links.initFile.push; post("/api/initfile/push", { assertEquals "Not available", it.message })

assertNull _links.runFile.save; post("/api/runfile", { assertEquals "Not available", it.message })

assertNull _links.review.promote; post("/api/review/promote", { assertEquals "Not available", it.message })

// Apply security token
getAsString("/api/security/apply?t=${securityToken}")

// Update links
_links = links()

assertNotNull _links.settings
get(_links.settings.default, {
    assertTrue it.security.enabled
})