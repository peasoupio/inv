package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.composer.WebServer

@CompileStatic
class ComposerCommand implements CliCommand {

    private final String initFileLocation

    ComposerCommand() {
        this(null)
    }

    ComposerCommand(String initFileLocation) {
        this.initFileLocation = initFileLocation
    }

    @Override
    int call(Map args = [:]) {
        if (args == null)
            throw new IllegalArgumentException("args")

        Map settings = [
                workspace: Home.getCurrent().absolutePath,
                initFile: this.initFileLocation
        ]

        // Check whether or not Composer will use the AppLauncher or classic approach.
        if (args["<port>"])
            settings.port = args["<port>"]

        // Check whether or not Composer will use the AppLauncher or classic approach.
        if (System.getProperty(WebServer.CONFIG_LAUNCHER))
            settings.appLauncher = System.getProperty(WebServer.CONFIG_LAUNCHER)

        return new WebServer(settings)
                .routes()
    }

    @Override
    boolean rolling() {
        return true
    }

    @Override
    String usage() {
        """
Start Composer at the current INV_HOME location.

Usage:
  inv [-dsx] composer [-p <port>]

Options:
  -p, --port=port
               Sets the listening port
    
Environment variables:
  ${WebServer.CONFIG_SSL_KEYSTORE}  Sets the SSL keystore location
  ${WebServer.CONFIG_SSL_PASSWORD}  Sets the SSL keystore password
"""
    }
}
