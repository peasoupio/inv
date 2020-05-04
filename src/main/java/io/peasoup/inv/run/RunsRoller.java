package io.peasoup.inv.run;

import io.peasoup.inv.Home;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public class RunsRoller {
    private static final RunsRoller latest = new RunsRoller();

    public static File runsFolder() {
        return new File(Home.getCurrent(), ".runs/");
    }

    public static RunsRoller getLatest() {
        // Make sure .runs/ exists
        if (runsFolder().mkdirs())
            Logger.system("Created runs (./runs) folder");

        return latest;
    }

    public File folder() {
        return new File(runsFolder(), latestIndex().toString());
    }

    public File failFolder() {
        return new File(runsFolder(), "latestFail/");
    }

    public File successFolder() {
        return new File(runsFolder(), "latestSuccess/");
    }

    private File latestSymlink() {
        return new File(runsFolder(), "latest/");
    }

    private RunsRoller() {
    }

    public void latestHaveFailed() throws IOException {
        if (!folder().exists()) return;

        if (failFolder().exists())
            Files.delete(failFolder().toPath());

        Files.createSymbolicLink(failFolder().toPath(), folder().getCanonicalFile().toPath());
    }

    public void latestHaveSucceed() throws IOException {
        if (!folder().exists()) return;

        if (successFolder().exists())
            Files.delete(successFolder().toPath());

        Files.createSymbolicLink(successFolder().toPath(), folder().getCanonicalFile().toPath());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void roll() throws IOException {
        int nextIndex = hasRuns()? latestIndex() + 1 : 1;
        File nextFolder = new File(runsFolder(), Integer.toString(nextIndex));

        // Clean symlink for previous roll
        if (latestSymlink().exists())
            Files.delete(latestSymlink().toPath());

        // Make sure it's clean
        FileUtils.deleteDirectory(nextFolder);
        nextFolder.mkdirs();

        // Create new symlink for new run folder
        Files.createSymbolicLink(latestSymlink().toPath(), nextFolder.getCanonicalFile().toPath());
    }

    private Integer latestIndex() {
        File[] files = runsFolder().listFiles();

        if (files == null || files.length == 0)
            return 1;

        return (int)Arrays.stream(files)
                .filter(file -> file.isDirectory() && StringUtils.isNumeric(file.getName()))
                .count();
    }

    private boolean hasRuns() {
        File[] files =  runsFolder().listFiles();

        if (files == null)
            return false;

        return files.length > 0;
    }


}
