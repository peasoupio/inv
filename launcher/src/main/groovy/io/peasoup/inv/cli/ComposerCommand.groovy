package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.SystemInfo
import io.peasoup.inv.composer.WebServer

import java.nio.file.Paths

@CompileStatic
class ComposerCommand implements CliCommand {

    private final Map initialSettings

    ComposerCommand() {
        this(null)
    }

    ComposerCommand(Map initialSettings) {
        this.initialSettings = initialSettings
    }

    @Override
    int call(Map args = [:]) {
        if (args == null)
            throw new IllegalArgumentException("args")

        // Get current JAR location
        File jarLocation = Paths.get(
                ComposerCommand.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile()

        Map settings = [
                appLauncher: jarLocation.absolutePath,
                version    : SystemInfo.version()
        ]

        // Add initial settings
        if (initialSettings != null)
            settings += initialSettings

        // Check whether or not Composer will use the AppLauncher or classic approach.
        if (args["<port>"])
            settings.port = args["<port>"]

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
