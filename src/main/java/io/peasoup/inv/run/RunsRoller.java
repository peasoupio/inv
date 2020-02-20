package io.peasoup.inv.run;

import io.peasoup.inv.Home;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public class RunsRoller {
    public static File runsFolder() {
        return new File(Home.getCurrent(), ".runs/");
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
        // Make sure .runs/ exists
        runsFolder().mkdirs();
    }

    public void latestHaveFailed() throws IOException {
        if (!folder().exists()) return;

        if (failFolder().exists()) {
            if (failFolder().isDirectory())
                ResourceGroovyMethods.deleteDir(failFolder());
            else
                assert failFolder().delete() : "Could not delete fail symlink";
        }

        Files.createSymbolicLink(failFolder().toPath(), folder().getCanonicalFile().toPath());
    }

    public void latestHaveSucceed() throws IOException {
        if (!folder().exists()) return;

        if (successFolder().exists()) {
            if (successFolder().isDirectory())
                ResourceGroovyMethods.deleteDir(successFolder());
            else
                assert successFolder().delete() : "Could not delete success symlink";
        }

        Files.createSymbolicLink(successFolder().toPath(), folder().getCanonicalFile().toPath());
    }

    public void roll() throws IOException {
        runsFolder().mkdirs();

        Integer nextInder = latestIndex() + 1;
        File nextFolder = new File(runsFolder(), nextInder.toString());

        // Make sure it's clean
        ResourceGroovyMethods.deleteDir(nextFolder);
        nextFolder.mkdirs();

        if (latestSymlink().exists()) {
            if (latestSymlink().isDirectory())
                ResourceGroovyMethods.deleteDir(latestSymlink());
            else
                assert latestSymlink().delete(): "Could not delete latest symlink";
        }

        Files.createSymbolicLink(latestSymlink().toPath(), nextFolder.getCanonicalFile().toPath());
    }

    private Integer latestIndex() {
        File[] files = runsFolder().listFiles();

        if (files.length == 0)
            return 0;

        return (int)Arrays.stream(files)
                .filter(file -> file.isDirectory() && StringUtils.isNumeric(file.getName()))
                .count() - 1;
    }

    public static RunsRoller getLatest() {
        return latest;
    }

    private static final RunsRoller latest = new RunsRoller();
}
