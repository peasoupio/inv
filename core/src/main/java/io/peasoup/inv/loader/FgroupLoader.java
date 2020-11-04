package io.peasoup.inv.loader;

import io.peasoup.fgroup.FileMatches;
import io.peasoup.fgroup.FileSeeker;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FgroupLoader {

    private static final FileSeeker fileSeeker =
            new FileSeeker(InvMatches.class.getResource("/fgroup.txt").getPath());

    private FgroupLoader() {
        // private ctor
    }

    /**
     * Find matches based on the fgroup.txt configuration for a directory.
     * @param dir Directory to lookup.
     * @return InvMatches object.
     */
    public static InvMatches findMatches(String dir) {
        if (StringUtils.isEmpty(dir)) throw new IllegalArgumentException("dir");

        FileMatches fileMatches;
        if (new File(dir, ".inv/").exists())
            fileMatches = fileSeeker.seek(dir + "/.inv/");
        else
            fileMatches = fileSeeker.seek(dir);

        InvMatches invMatches = new InvMatches(dir);

        // Load groovy files right now
        for(FileMatches.FileMatchRecord match : fileMatches.get("groovyFiles")) {
            invMatches.getGroovyFiles().add(match.getCurrent());
        }

        // Load groovy test files if included
        for(FileMatches.FileMatchRecord match : fileMatches.get("groovyTestFiles")) {
            invMatches.getGroovyTestFiles().add(match.getCurrent());
        }

        // Add invs
        for(FileMatches.FileMatchRecord match : fileMatches.get("invFiles")) {
            invMatches.getInvFiles().add(match.getCurrent());
        }

        // Add repo file
        for(FileMatches.FileMatchRecord match : fileMatches.get("repoFile")) {
            invMatches.setRepoFile(match.getCurrent());
        }

        return invMatches;
    }

    public static class InvMatches {
        private final String rootPath;
        private final List<Path> groovyFiles;
        private final List<Path> groovyTestFiles;
        private final List<Path> invFiles;
        private Path repoFile;

        public InvMatches(String rootPath) {
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
