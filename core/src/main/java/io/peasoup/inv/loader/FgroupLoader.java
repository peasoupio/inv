package io.peasoup.inv.loader;

import io.peasoup.fgroup.FileMatches;
import io.peasoup.fgroup.FileSeeker;
import io.peasoup.inv.Logger;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FgroupLoader {

    private static final FileSeeker fileSeeker;


    static {
        String tmpFolder = System.getProperty("java.io.tmpdir");
        Path configFile = Path.of(tmpFolder, "fgroup.txt");

        try (InputStream is = FgroupLoader.class.getResourceAsStream("/fgroup.txt")) {
            // Copy existing local
            if (Files.exists(configFile))
                Files.delete(configFile);

            // Copy from classpath to local
            Files.copy(is, configFile);
        } catch (IOException e) {
            Logger.error(e);
        }

        fileSeeker = new FileSeeker(configFile.toString());
    }

    private FgroupLoader() {
        // private ctor

    }

    /**
     * Find matches based on the fgroup.txt configuration for a directory.
     *
     * @param dir Directory to lookup.
     * @return RepoMatches object.
     */
    public static RepoMatches findMatches(String dir) {
        if (StringUtils.isEmpty(dir)) throw new IllegalArgumentException("dir");

        FileMatches fileMatches;
        if (new File(dir, ".inv/").exists())
            fileMatches = fileSeeker.seek(dir + "/.inv/");
        else
            fileMatches = fileSeeker.seek(dir);

        RepoMatches repoMatches = new RepoMatches(dir);

        // Load groovy files right now
        for (FileMatches.FileMatchRecord match : fileMatches.get("groovyFiles")) {
            repoMatches.getGroovyFiles().add(match.getMatch());
        }

        // Load groovy test files if included
        for (FileMatches.FileMatchRecord match : fileMatches.get("groovyTestFiles")) {
            repoMatches.getGroovyTestFiles().add(match.getMatch());
        }

        // Add invs
        for (FileMatches.FileMatchRecord match : fileMatches.get("invFiles")) {
            repoMatches.getInvFiles().add(match.getMatch());
        }

        // Add repo file
        for (FileMatches.FileMatchRecord match : fileMatches.get("repoFile")) {
            repoMatches.setRepoFile(match.getMatch());
        }

        // Add deps file
        for (FileMatches.FileMatchRecord match : fileMatches.get("grabFile")) {
            repoMatches.setGrabFile(match.getMatch());
        }

        return repoMatches;
    }

    @Getter
    public static class RepoMatches {
        private final String rootPath;
        private final List<Path> groovyFiles;
        private final List<Path> groovyTestFiles;
        private final List<Path> invFiles;

        @Setter
        private Path repoFile;

        @Setter
        private Path grabFile;

        public RepoMatches(String rootPath) {
            if (StringUtils.isEmpty(rootPath))
                throw new IllegalArgumentException("rootPath");

            this.rootPath = rootPath;
            this.groovyFiles = new ArrayList<>();
            this.groovyTestFiles = new ArrayList<>();
            this.invFiles = new ArrayList<>();
        }
    }
}
