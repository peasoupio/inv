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
        return new File(Home.current, ".runs/");
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


        if (Files.isSymbolicLink(failFolder().toPath())) failFolder().delete();
        else ResourceGroovyMethods.deleteDir(failFolder());

        Files.createSymbolicLink(failFolder().toPath(), folder().getCanonicalFile().toPath());
    }

    public void latestHaveSucceed() throws IOException {
        if (!folder().exists()) return;


        if (Files.isSymbolicLink(successFolder().toPath())) successFolder().delete();
        else ResourceGroovyMethods.deleteDir(successFolder());

        Files.createSymbolicLink(successFolder().toPath(), folder().getCanonicalFile().toPath());
    }

    public void roll() throws IOException {
        runsFolder().mkdirs();

        Integer nextInder = latestIndex() + 1;
        File nextFolder = new File(runsFolder(), nextInder.toString());

        // Make sure it's clean
        ResourceGroovyMethods.deleteDir(nextFolder);
        nextFolder.mkdirs();

        if (Files.isSymbolicLink(latestSymlink().toPath())) latestSymlink().delete();
        else ResourceGroovyMethods.deleteDir(latestSymlink());

        Files.createSymbolicLink(latestSymlink().toPath(), nextFolder.getCanonicalFile().toPath());
    }

    private Integer latestIndex() {
        File[] files = runsFolder().listFiles();

        if (files.length == 0)
            return 0;

        return (int)Arrays.stream(files)
                .filter(file -> file.isDirectory() && StringUtils.isNumeric(file.getName()))
                .count();
    }

    public static RunsRoller getLatest() {
        return latest;
    }

    private static final RunsRoller latest = new RunsRoller();
}
