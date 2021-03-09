package io.peasoup.inv.composer.run1

@groovy.transform.BaseScript(groovy.util.DelegatingScript.class)
import static org.junit.Assert.*

// Get links
Map links = links()

// Wait until booting
waitFor(10) {
    get(links.setup) { it.booted }
}

// Search using default
get(links.run.default) {
    assertNotNull it
    assertEquals 5, it.total
    assertEquals 5, it.count
    assertEquals 0, it.staged
    assertEquals 0, it.required

    assertNotNull it.names
    assertTrue it.names.contains("StatementA1")
    assertTrue it.names.contains("StatementA2")
    assertTrue it.names.contains("StatementB1")
    assertTrue it.names.contains("StatementB2")
    assertTrue it.names.contains("StatementC1")

    assertNotNull it.nodes
    assertEquals 5, it.nodes.size()
}

// Stage all
post(links.run.staged) { assertEquals 0, it.count}
post(links.run.stageAll)
post(links.run.staged) { assertEquals 5, it.count }
post(links.run.unstageAll)
post(links.run.staged) { assertEquals 0, it.count}

// Search by name
post(links.run.search, [name: 'StatementB1']) {
    assertNotNull it
    assertEquals 5, it.total
    assertEquals 1, it.count

    assertNotNull it.owners
    assertEquals 1, it.owners.size()
    assertEquals "ScriptY", it.owners[0]
}

// Search by owner
post(links.run.search, [owner: 'ScriptZ']) {
    assertNotNull it
    assertEquals 5, it.total
    assertEquals 1, it.count

    assertNotNull it.nodes
    assertEquals 1, it.nodes.size()
    assertEquals "StatementC1", it.nodes[0].name
}

// Search by ID
Map idLink
post(links.run.search, [id: 'undefined']) {
    assertNotNull it
    assertEquals 5, it.total
    assertEquals 5, it.count // fetched all

    idLink = it.nodes[0]
}

// Stage id
post(links.run.staged) { assertEquals 0, it.count}
post(idLink.links.stage)
post(links.run.staged) { assertEquals 1, it.count }
post(idLink.links.unstage)
post(links.run.staged) { assertEquals 0, it.count}

// Get owners
Map ownerLink
get(links.run.owners) {
    assertNotNull it
    assertEquals 3, it.size()

    ownerLink = it[0]
    assertNotNull ownerLink.owner
    assertNotNull ownerLink.stagedBy
    assertNotNull ownerLink.requiredBy
    assertNotNull ownerLink.links
}

// Stage owner
post(links.run.staged) { assertEquals 0, it.count}
post(ownerLink.links.stage)
post(links.run.staged) { assertEquals 2, it.count}
post(ownerLink.links.unstage)
post(links.run.staged) { assertEquals 0, it.count}

// Get names
Map nameLink
get(links.run.names) {
    assertNotNull it
    assertEquals 5, it.size()

    nameLink = it[0]
    assertNotNull nameLink.name
    assertNotNull nameLink.links
}

// Stage name
post(links.run.staged) { assertEquals 0, it.count}
post(nameLink.links.stage)
post(links.run.staged) { assertEquals 1, it.count }
post(nameLink.links.unstage)
post(links.run.staged) { assertEquals 0, it.count}

// Get tags
Map tagsLink
Map subTagsLink
get(links.run.tags) {
    assertNotNull it
    assertNotNull it.tags
    assertEquals 1, it.tags.size()

    tagsLink = it.tags[0]
    assertEquals "tag", tagsLink.label

    subTagsLink = tagsLink.subTags[0]
    assertEquals "A", subTagsLink.label
    assertEquals "B", tagsLink.subTags[1].label
}

// Stage tag and subtag
post(links.run.staged) { assertEquals 0, it.count }
post(subTagsLink.links.stageAll)
post(links.run.staged) { assertEquals 4, it.count }
post(subTagsLink.links.unstageAll)
post(links.run.staged) { assertEquals 0, it.count }