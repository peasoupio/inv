package io.peasoup.inv.composer.repo2

@groovy.transform.BaseScript(groovy.util.DelegatingScript.class)
import static org.junit.Assert.*

// Get links
Map links = links()

// Wait until booting
waitFor(10) {
    get(links.setup) { it.booted }
}

// Add a new Groovy REPO
def groovySourceText = """
repo {
    name 'repoX'
    path 'path'
    src 'my-src'
}
"""

post("${links.repos.add}?name=repoX&mimeType=text/x-groovy".toString(), groovySourceText, {
    assertEquals 0, it.errorCount

    post(links.repos.search, [name: "repoX"], {
        assertEquals 1, it.count

        get(it.descriptors[0].links.default, {
            assertEquals groovySourceText, it.script.text
        })
    })

})

// Add a new Yaml REPO
def yamlSourceText = """
repo:
  - name: repoY
    hooks:
      init: |
        echo 'ok'
"""

post("${links.repos.add}?name=repoY&mimeType=text/x-yaml".toString(), yamlSourceText, {
    assertEquals 0, it.errorCount

    post(links.repos.search, [name: "repoY"], {
        assertEquals 1, it.count

        get(it.descriptors[0].links.default, {
            assertEquals yamlSourceText, it.script.text
        })
    })
})

// Remove source
post(links.repos.search, [name: "repoX"], {
    assertEquals 1, it.count

    post(it.descriptors[0].links.remove, {
        assertEquals "Deleted", it.result
    })

    post(links.repos.search, [name: "repoX"], {
        assertEquals 0, it.count
    })
})

post(links.repos.search, [name: "repoY"], {
    post(it.descriptors[0].links.remove, {
        assertEquals "Deleted", it.result
    })
})