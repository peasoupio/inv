package io.peasoup.inv

import groovy.transform.CompileStatic
import io.peasoup.inv.io.FileUtils

import java.nio.file.Files

@CompileStatic
class SystemInfo {

    static String version() {
        Properties releaseInfo = new Properties()
        def releaseFile = SystemInfo.getResourceAsStream("/release.properties")
        if (!releaseFile)
            return

        releaseInfo.load(releaseFile)

        return releaseInfo.version
    }

    /**
     * Check whether or not the system fails to meet the minimal "consistency" requirements
     * @param main Main class instance
     * @return true if fails, otherwise false
     */
    static boolean consistencyFails() {
        if (checkInvHome(Home.getCurrent()))
            return true

        if (checkReleaseInfo(version()))
            return true

        return false
    }

    protected static boolean checkInvHome(File invHome) {
        if (invHome == null) {
            Logger.fail "INV_HOME is not valid"
            return true
        }

        Logger.system "[VARS] INV_HOME: ${FileUtils.convertUnixPath(invHome.absolutePath)}"

        if (!invHome.exists()) {
            Logger.fail "INV_HOME does not exists"
            return true
        }

        if (!invHome.isDirectory()) {
            Logger.fail "INV_HOME is not a directory"
            return true
        }

        if (!Files.isWritable(invHome.toPath())) {
            Logger.fail "current user is not able to read from INV_HOME"
            return true
        }

        return false
    }

    protected static boolean checkReleaseInfo(String version) {
        if (!version) {
            Logger.fail "couldn't get current version"
            return true
        }

        Logger.system "[VARS] VERSION: ${version}"

        return false
    }
}
