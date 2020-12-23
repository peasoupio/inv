package io.peasoup.inv.composer.boot2

import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration

@groovy.transform.BaseScript(groovy.util.DelegatingScript.class)
import static junit.framework.TestCase.*

// Get links
Map links = links()

// Wait until booting
waitFor(10) {
    get(links.setup) { it.booted }
}


get(links.repos.default) {
    assertNotNull it
    assertNotNull it.descriptors
    assertEquals 2, it.total

    assertTrue it.descriptors.any { it.name.equals("io.peasoup.inv.pub.net.http") }
    assertTrue it.descriptors.any { it.name.equals("repo1") }
}