package io.peasoup.inv.loader;

import io.peasoup.fgroup.FileMatches;
import io.peasoup.fgroup.FileSeeker;
import io.peasoup.inv.Logger;
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

        if (!Files.exists(configFile)) {
            try (InputStream is = FgroupLoader.class.getResourceAsStream("/fgroup.txt");) {
                Files.copy(is, configFile);
            } catch (IOException e) {
                Logger.error(e);
            }
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
            repoMatches.getGroovyFiles().add(match.getCurrent());
        }

        // Load groovy test files if included
        for (FileMatches.FileMatchRecord match : fileMatches.get("groovyTestFiles")) {
            repoMatches.getGroovyTestFiles().add(match.getCurrent());
        }

        // Add invs
        for (FileMatches.FileMatchRecord match : fileMatches.get("invFiles")) {
            repoMatches.getInvFiles().add(match.getCurrent());
        }

        // Add repo file
        for (FileMatches.FileMatchRecord match : fileMatches.get("repoFile")) {
            repoMatches.setRepoFile(match.getCurrent());
        }

        return repoMatches;
    }

    public static class RepoMatches {
        private final String rootPath;
        private final List<Path> groovyFiles;
        private final List<Path> groovyTestFiles;
        private final List<Path> invFiles;
        private Path repoFile;

        public RepoMatches(String rootPath) {
            if (StringUtils.isEmpty(rootPath))
                throw new IllegalArgumentException("rootPath");

            this.rootPath = rootPath;
            this.groovyFiles = new ArrayList<>();
            this.groovyTestFiles = new ArrayList<>();
            this.invFiles = new ArrayList<>();
        }

        public String getRootPath() {
            return rootPath;
        }

        public List<Path> getGroovyFiles() {
            return groovyFiles;
        }

        public List<Path> getGroovyTestFiles() {
            return groovyTestFiles;
        }

        public List<Path> getInvFiles() {
            return invFiles;
        }

        public void setRepoFile(Path repoFile) {
            this.repoFile = repoFile;
        }

        public Path getRepoFile() {
            return repoFile;
        }
    }
}
