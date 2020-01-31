package io.peasoup.inv.run

import groovy.transform.CompileStatic
import io.peasoup.inv.Main

import java.nio.file.Files

@CompileStatic
class RunsRoller {

    static File runsFolder() { return new File(Main.currentHome,".runs/") }
    static final RunsRoller latest = new RunsRoller()

    File folder() { new File(runsFolder(), latestIndex().toString()) }

    File failFolder() { return new File(runsFolder(), "latestFail/") }
    File successFolder() { return new File(runsFolder(), "latestSuccess/") }

    private File latestSymlink() { return new File(runsFolder(), "latest/") }

    private RunsRoller() {
        // Make sure .runs/ exists
        runsFolder().mkdirs()
    }

    void latestHaveFailed() {
        if (!folder().exists())
            return

        if (Files.isSymbolicLink(failFolder().toPath()))
            failFolder().delete()
        else
            failFolder().deleteDir()

        Files.createSymbolicLink(failFolder().toPath(), folder().canonicalFile.toPath())
    }

    void latestHaveSucceed() {
        if (!folder().exists())
            return

        if (Files.isSymbolicLink(successFolder().toPath()))
            successFolder().delete()
        else
            successFolder().deleteDir()

        Files.createSymbolicLink(successFolder().toPath(), folder().canonicalFile.toPath())
    }

    void roll() {
        runsFolder().mkdirs()

        File nextFolder = new File(runsFolder(), (latestIndex() + 1).toString())

        // Make sure it's clean
        nextFolder.deleteDir()
        nextFolder.mkdirs()

        if (Files.isSymbolicLink(latestSymlink().toPath()))
            latestSymlink().delete()
        else
            latestSymlink().deleteDir()

        Files.createSymbolicLink(latestSymlink().toPath(), nextFolder.canonicalFile.toPath())
    }

    private Integer latestIndex() {
        def files = runsFolder().listFiles()

        if (!files)
            return 0

        Integer index = files
                .findAll { it.isDirectory() && it.name.isInteger() }
                .collect { it.name as Integer  }
                .max() ?: 0

        return index
    }
}
