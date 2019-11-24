package io.peasoup.inv

class SystemChecks {

    /**
     * Check whether or not the system fails to meet the minimal "consistency" requirements
     * @param main Main class instance
     * @return true if fails, otherwise false
     */
    boolean consistencyFails(Main main) {
        assert main

        return checkInvHome(main.invHome) &&
               checkCache(new File(InvInvoker.Cache))
    }

    boolean checkInvHome(File invHome) {
        if (invHome == null) {
            Logger.fail "INV_HOME is not valid"
            return true
        }

        Logger.debug "INV_HOME: ${invHome.absolutePath}"

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

    boolean checkCache(File cache) {
        if (cache == null) {
            Logger.fail "Cache does not exists"
            return true
        }

        Logger.debug "Cache: ${cache.absolutePath}"

        if (cache.exists() && !cache.isDirectory()) {
            Logger.fail "Cache is not a directory"
            return true
        }

        if (!cache.canWrite()) {
            Logger.fail "current user is not able to write in cache"
            return true
        }

        return false
    }
}
