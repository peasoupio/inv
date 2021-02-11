package io.peasoup.inv.composer.repo1

@groovy.transform.BaseScript(groovy.util.DelegatingScript.class)
import static org.junit.Assert.*

// Get links
Map links = links()

// Wait until booting
waitFor(10) {
    get(links.setup) { it.booted }
}

// Get all repos
post(links.repos.search) {
    assertNotNull it
    assertEquals 3, it.total
}

// Stage all repos
post(links.repos.search) { assertEquals 0, it.staged }
post(links.repos.stageAll)
post(links.repos.search) { assertEquals 3, it.staged }
post(links.repos.unstageAll)
post(links.repos.search) { assertEquals 0, it.staged }

// Get repo
Map repoLink
post(links.repos.search) {
    assertNotNull it
    repoLink = it.descriptors[0]
}

// Stage repo
post(links.repos.search) { assertEquals 0, it.staged }
post(repoLink.links.stage)
post(links.repos.search) { assertEquals 1, it.staged }
post(repoLink.links.unstage)
post(links.repos.search) { assertEquals 0, it.staged }