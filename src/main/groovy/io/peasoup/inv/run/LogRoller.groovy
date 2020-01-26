package io.peasoup.inv.run

import io.peasoup.inv.Main

import java.nio.file.Files

class LogRoller {

    static File runsFolder() { return new File(Main.currentHome,".runs/") }
    static final LogRoller latest = new LogRoller()

    File folder() { new File(runsFolder(), latestIndex().toString()) }

    File failFolder() { return new File(runsFolder(), "latestFail/") }
    File successFolder() { return new File(runsFolder(), "latestSuccess/") }

    private File latestSymlink() { return new File(runsFolder(), "latest/") }

    private LogRoller() {
        // Rolling .runs/ files
        push()
    }

    void latestHaveFailed() {
        if (!folder().exists())
            return

        failFolder().delete()
        Files.createSymbolicLink(failFolder().toPath(), folder().canonicalFile.toPath())
    }

    void latestHaveSucceed() {
        if (!folder().exists())
            return

        successFolder().delete()
        Files.createSymbolicLink(successFolder().toPath(), folder().canonicalFile.toPath())
    }

    private void push() {
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
        Integer index = runsFolder().listFiles()
                .findAll { it.isDirectory() && it.name.isInteger() }
                .collect { it.name as Integer  }
                .max() ?: 0

        return index
    }
}
