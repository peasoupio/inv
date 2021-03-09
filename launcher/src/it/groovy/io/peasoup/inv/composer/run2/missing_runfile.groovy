package io.peasoup.inv.composer.run2

@groovy.transform.BaseScript(groovy.util.DelegatingScript.class)
import static org.junit.Assert.*

// Get links
Map links = links()

// Wait until booting
waitFor(10) {
    get(links.setup) { it.booted }
}

// (GET) Expect "no run file" error message
[
        links.run.default,
        links.run.search,
        links.run.owners,
        links.run.names,
        links.run.tree,
        links.run.tags,
        "/api/run/requiredBy",
        links.runFile.default
].each {
    get(it) {
        assertNotNull it
        assertEquals "Run is not ready yet", it.message
    }
}

// (POST) Expect "no run file" error message
[
        links.run.default,
        links.run.staged,
        links.run.stageAll,
        links.run.unstageAll,
        "/api/run/stage",
        "/api/run/unstage",
        "/api/run/tags/stage",
        "/api/run/tags/unstage"
].each {
    post(it) {
        assertNotNull it
        assertEquals "Run is not ready yet", it.message
    }
}

println "ok"

