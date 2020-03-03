package io.peasoup.inv

import groovy.transform.CompileStatic
import io.peasoup.inv.run.Logger

@CompileStatic
class SystemChecks {

    /**
     * Check whether or not the system fails to meet the minimal "consistency" requirements
     * @param main Main class instance
     * @return true if fails, otherwise false
     */
    boolean consistencyFails(Main main) {
        assert main, 'Main is required'

        return checkInvHome(Home.getCurrent())
    }

    boolean checkInvHome(File invHome) {
        if (invHome == null) {
            Logger.fail "INV_HOME is not valid"
            return true
        }

        Logger.debug "[PARAMS] INV_HOME: ${invHome.absolutePath}"

        if (!invHome.exists()) {
            Logger.fail "INV_HOME does not exists"
            return true
        }

        if (!invHome.isDirectory()) {
            Logger.fail "INV_HOME is not a directory"
            return true
        }

        if (!invHome.canRead()) {
            Logger.fail "current user is not able to read from INV_HOME"
            return true
        }

        return false
    }
}
