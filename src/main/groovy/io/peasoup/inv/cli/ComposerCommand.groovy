package io.peasoup.inv.cli

import io.peasoup.inv.Main
import io.peasoup.inv.composer.WebServer

class ComposerCommand {

    static int call() {
        return new WebServer(workspace: Main.currentHome.absolutePath)
                .map()
    }
}
