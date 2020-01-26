package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Main
import io.peasoup.inv.composer.WebServer

@CompileStatic
class ComposerCommand implements CliCommand {

    int call() {
        return new WebServer(workspace: Main.currentHome.absolutePath)
                .map()
    }

    boolean rolling() {
        return false
    }
}
