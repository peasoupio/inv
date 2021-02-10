package io.peasoup.inv.composer.repo1

@groovy.transform.BaseScript(groovy.util.DelegatingScript.class)
import static org.junit.Assert.*

// Get links
Map links = links()

// Wait until booting
waitFor(10) {
    get(links.setup) { it.booted }
}


// Search 1 repo
post(links.repos.search, [name: "repoB"], {
    assertEquals 1, it.count
    assertEquals 3, it.total

    assertNotNull it.descriptors
    assertEquals 1, it.descriptors.size()

    assertEquals "repoB", it.descriptors[0].name
})

// Search 3 (all) repos
post(links.repos.search, [name: "repo"], {
    assertEquals 3, it.count
    assertEquals 3, it.total

    assertNotNull it.descriptors
    assertEquals 3, it.descriptors.size()

    assertTrue it.descriptors.any { it.name == "repoA" }
    assertTrue it.descriptors.any { it.name == "repoB" }
    assertTrue it.descriptors.any { it.name == "repoC" }
})

// View one REPO from a parameters result
post(links.repos.search, [name: "repoB"], {
    get(it.descriptors[0].links.default, {
        assertEquals "repoB", it.name
    })
})