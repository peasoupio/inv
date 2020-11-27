package io.peasoup.inv.composer.boot2

import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration

@groovy.transform.BaseScript(groovy.util.DelegatingScript.class)
import static junit.framework.TestCase.*

String setupAPI
String repoDefault

get("/api/v1") {
    setupAPI = it.links.setup
    repoDefault = it.links.repos.default
}

int i = 10
while(i > 0) {
    sleep(1000)
    get(setupAPI) {
        if (it.booted)
            i = 0
    }
    i--
}

get(repoDefault) {
    assertNotNull it
    assertNotNull it.descriptors
    assertEquals 2, it.total

    assertTrue it.descriptors.any { it.name.equals("io.peasoup.public.net.http") }
    assertTrue it.descriptors.any { it.name.equals("repo1") }
}