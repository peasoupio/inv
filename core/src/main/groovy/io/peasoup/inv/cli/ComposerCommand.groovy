package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.composer.WebServer
import io.peasoup.inv.run.Logger

@CompileStatic
class ComposerCommand implements CliCommand {

    Map settings

    int call() {
        if (!settings)
            settings = [:]

        if (!settings.workspace)
            settings.workspace = Home.getCurrent().absolutePath

        // Check whether or not Composer will use the AppLauncher or classic approach.
        settings.appLauncher = System.getenv("APPLAUNCHER")
        if (settings.appLauncher)
            Logger.system "[AppLauncher] location: ${settings.appLauncher}"

        return new WebServer(settings)
                .routes()
    }

    boolean rolling() {
        return true
    }
}
