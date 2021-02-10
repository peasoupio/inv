package io.peasoup.inv.composer.repo3

@groovy.transform.BaseScript(groovy.util.DelegatingScript.class)
import static org.junit.Assert.*

// Get links
Map links = links()

// Wait until booting
waitFor(10) {
    get(links.setup) { it.booted }
}

// Make sure all reminiscent parameters files are deleted (mostly to help working with this script)
post(links.repos.resetAll)

// Stage all repos
post(links.repos.stageAll)

// Apply default to all then reset
post(links.repos.search, [name: "repo"], {
    assertEquals 3, it.count

    for(Map descriptor: it.descriptors) {
        get(descriptor.links.default, {
            assertNotNull it.parameters
            assertEquals 1, it.parameters.size()

            assertNull it.parameters[0].value
        })
    }

    post(links.repos.applyDefaultAll)

    for(Map descriptor: it.descriptors) {
        get(descriptor.links.default, {
            assertNotNull it.parameters
            assertEquals 1, it.parameters.size()

            assertEquals it.parameters[0].defaultValue, it.parameters[0].value
        })
    }

    post(links.repos.resetAll)

    for(Map descriptor: it.descriptors) {
        get(descriptor.links.default, {
            assertNotNull it.parameters
            assertEquals 1, it.parameters.size()

            assertNull it.parameters[0].value
        })
    }
})

// Apply single value
post(links.repos.search, [name: "repoA"], {
    assertEquals 1, it.count

    // Set new value
    get(it.descriptors[0].links.default, {
        post(it.parameters[0].links.save, [parameterValue: "myNewValue"], {
            assertEquals "Updated", it.result
        })
    })

    // Check if new value has been properly updated
    get(it.descriptors[0].links.default, {
        assertNotNull it.parameters[0]

        assertEquals "myNewValue", it.parameters[0].value
    })

})

// Get parameters values (static)
post(links.repos.search, [name: "repoC"], {
    assertEquals 1, it.count

    get(it.descriptors[0].links.parametersValues, {
        assertNotNull it.parameterC

        assertTrue it.parameterC.contains("my")
        assertTrue it.parameterC.contains("parameter")
        assertTrue it.parameterC.contains("values")
    })
})