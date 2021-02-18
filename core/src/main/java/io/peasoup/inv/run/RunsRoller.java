package io.peasoup.inv.run;

import io.peasoup.inv.Home;
import io.peasoup.inv.Logger;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.OptionalInt;

public class RunsRoller {
    private static final RunsRoller latest = new RunsRoller();

    /**
     * Gets the latest roller instance.
     * @return Singleton instance of RunsRoller.
     */
    public static RunsRoller getLatest() {
        // Make sure .runs/ exists
        if (!Home.getRunsFolder().mkdirs() && !Home.getRunsFolder().exists())
            Logger.warn("Could not create runs folder");

        return latest;
    }

    /**
     * Force delete the runs folder.
     * @return True if deleted, otherwise false.
     */
    public static boolean forceDelete() {
        try {
            getLatest().closeRunfileOutputStream();
            ResourceGroovyMethods.deleteDir(Home.getRunsFolder());

            return true;
        } catch (IOException e) {
            Logger.error(e);
            return false;
        }
    }

    public File folder() {
        return new File(Home.getRunsFolder(), latestIndex().toString());
    }

    public File failFolder() {
        return new File(Home.getRunsFolder(), "latestFail/");
    }

    public File successFolder() {
        return new File(Home.getRunsFolder(), "latestSuccess/");
    }

    private File latestSymlink() {
        return new File(Home.getRunsFolder(), "latest/");
    }

    private OutputStream latestRunFileOutputStream;

    private RunsRoller() {
    }

    public void latestHaveFailed() throws IOException {
        if (!folder().exists()) return;

        if (failFolder().exists())
            Files.delete(failFolder().toPath());

        try {
            Files.createSymbolicLink(failFolder().toPath(), folder().getCanonicalFile().toPath());
        } catch(IOException ex) {
            Logger.warn("Cannot create 'latestFailed' symbolic link.");
        }
    }

    public void latestHaveSucceed() throws IOException {
        if (!folder().exists()) return;

        if (successFolder().exists())
            Files.delete(successFolder().toPath());

        try {
            Files.createSymbolicLink(successFolder().toPath(), folder().getCanonicalFile().toPath());
        } catch(IOException ex) {
            Logger.warn("Cannot create 'latestSucceed' symbolic link.");
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void roll() throws IOException {
        int nextIndex = hasRuns()? latestIndex() + 1 : 1;
        File nextFolder = new File(Home.getRunsFolder(), Integer.toString(nextIndex));

        // Clean symlink for previous roll
        if (latestSymlink().exists())
            Files.delete(latestSymlink().toPath());

        // Make sure it's clean
        ResourceGroovyMethods.deleteDir(nextFolder);
        nextFolder.mkdirs();

        // Create new symlink for new run folder
        try {
            Files.createSymbolicLink(latestSymlink().toPath(), nextFolder.getCanonicalFile().toPath());
        } catch(IOException ex) {
            Logger.warn("Cannot create 'latest' symbolic link.");
        }

        configureRunfileOutputStreams();
    }

    private Integer latestIndex() {
        File[] files = Home.getRunsFolder().listFiles();

        if (files == null || files.length == 0)
            return 0;

        OptionalInt max = Arrays.stream(files)
                .filter(file -> file.isDirectory() && StringUtils.isNumeric(file.getName()))
                .mapToInt(file -> Integer.parseInt(file.getName()))
                .max();

        if (max.isPresent()) {
            return max.getAsInt();
        }

        return 0;
    }

    private boolean hasRuns() {
        File[] files =  Home.getRunsFolder().listFiles();

        if (files == null)
            return false;

        return files.length > 0;
    }

    /**
     * Enables the single logging file
     */
    private void configureRunfileOutputStreams() {
        try {
            closeRunfileOutputStream();

            // Create a new run file FOS
            File outputFile = new File(latest.folder(), "run.txt");
            latestRunFileOutputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    latestRunFileOutputStream.flush();
                }
                catch (Throwable t) {
                    // Ignore
                }
            }, "Shutdown hook, thread flushing run file: " + outputFile.getAbsolutePath()));

            // Redirect Out and Err to FOS.
            TeeOutputStream runFileOut = new TeeOutputStream(System.out, latestRunFileOutputStream);
            PrintStream psOut = new PrintStream(runFileOut, true);
            System.setOut(psOut);

            TeeOutputStream runFileErr = new TeeOutputStream(System.err, latestRunFileOutputStream);
            PrintStream psErr = new PrintStream(runFileErr, true);
            System.setErr(psErr);

        } catch (Exception e) {
            Logger.error(e);
        }
    }

    private void closeRunfileOutputStream() throws IOException {
        // Make sure latest run file is flushed and closed.
        if (latestRunFileOutputStream != null) {
            latestRunFileOutputStream.flush();
            latestRunFileOutputStream.close();
            latestRunFileOutputStream = null;
        }
    }
}
