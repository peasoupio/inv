package io.peasoup.inv.composer

import groovy.transform.CompileStatic
import io.peasoup.inv.Logger
import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang.StringUtils
import org.apache.commons.validator.routines.EmailValidator
import spark.Request

import java.util.regex.Pattern

@CompileStatic
class Security {

    final static String COOKIE_AUTH_NAME = "auth"

    private final Settings settings
    private final String generatedToken

    Security(Settings settings) {
        if (settings == null)
            throw new IllegalArgumentException("settings")

        this.settings = settings

        if (isSecurityless())
            this.generatedToken = null
        else
            this.generatedToken = generateToken()
    }

    boolean isRequestSecure(Request req) {
        if (!req)
            return false

        if (isSecurityless())
            return true

        return isTokenValid(req.cookie(Security.COOKIE_AUTH_NAME))
    }

    boolean isTokenValid(String token) {
        if (StringUtils.isEmpty(token))
            return false

        return generatedToken == token
    }

    /**
     * Gets whether or not the current instance is running security less, meaning no security features are enabled.
     * @return True if securityless, otherwise false
     */
    boolean isSecurityless() {
        return !settings.isSecurityEnabled()
    }

    void print(int port, boolean usingSsl) {
        if (isSecurityless())
            return

        Logger.trace "[COMPOSER] Open the following URL in your browser:"
        Logger.trace "[COMPOSER] \t${usingSsl? "https" : "http"}://localhost:${port}"
        Logger.trace "[COMPOSER] For administrative privileges, open the following URL in your browser: "
        Logger.trace "[COMPOSER] \t${usingSsl? "https" : "http"}://localhost:${port}/api/security/apply?t=${generatedToken}"
    }

    /**
     * Generate a new authentication token
     * @return Alphanumeric generated token
     */
    private static String generateToken() {
        return RandomStringUtils.random(128, true, true)
    }
}
