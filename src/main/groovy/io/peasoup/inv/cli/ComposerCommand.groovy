package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.composer.WebServer

@CompileStatic
class ComposerCommand implements CliCommand {

    int call() {
        return new WebServer(workspace: Home.current.absolutePath)
                .map()
    }

    boolean rolling() {
        return false
    }
}
