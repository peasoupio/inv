package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.composer.WebServer

@CompileStatic
class ComposerCommand implements CliCommand {

    Map settings

    int call() {
        if (!settings)
            settings = [:]

        if (!settings.workspace)
            settings.workspace = Home.getCurrent().absolutePath

        return new WebServer(settings)
                .routes()
    }

    boolean rolling() {
        return false
    }
}
