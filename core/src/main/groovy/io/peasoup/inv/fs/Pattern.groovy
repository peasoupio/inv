package io.peasoup.inv.fs

import groovy.io.FileType
import io.peasoup.inv.io.FileUtils
import java.util.regex.Pattern as regexPattern

class Pattern {

    private Pattern() {

    }

    static List<File> get(List<String> patterns, List<String> excludes = [], File root = null, boolean recursive = true) {
        assert patterns != null, 'Patterns is required.'
        assert excludes != null, 'Exclude is required. Can be empty'

        // Get patterns from excludes
        List<regexPattern> excludePatterns = []
        for(String exclude : excludes) {
            String excludePattern =  FileUtils.convertUnixPath(exclude)
                    .replace("/", "\\/")
                    .replace(".", "\\.")
                    .replace("*", ".*")
                    .replace("?", ".")

            excludePatterns.add(regexPattern.compile(/.*${excludePattern}.*/))
        }

        def patternSearch = new PatternSearch(excludePatterns, root, recursive)

        // Do the actual lookup
        return patterns.collectMany(patternSearch.&lookup) as List<File>
    }

    private static class PatternSearch {

        private final List<regexPattern> excludePatterns
        private final File root
        private final boolean recursive

        PatternSearch(
                List<regexPattern> excludePatterns,
                File root,
                boolean recursive) {

            this.excludePatterns = excludePatterns
            this.root = root
            this.recursive = recursive
        }

        Collection<File> lookup(String lookupPattern) {
            // Remove leading ./ since it is redundant
            if (lookupPattern.startsWith("./") || lookupPattern.startsWith(".\\"))
                lookupPattern = lookupPattern.substring(2)

            // If pattern means an actual file, use it
            File lookupFile = new File(lookupPattern)
            if (!lookupFile.isDirectory() && lookupFile.exists())
                return [lookupFile]

            // Can't process Ant-compliant patterns without a root
            if (!root || !root.exists())
                return []

            // If patterns mean an actual file the root, use it
            File lookupFileRoot = new File(root, lookupPattern)
            if (!lookupFileRoot.isDirectory() && lookupFileRoot.exists())
                return [lookupFileRoot]

            // Convert Ant pattern to regex
            def includePatternStr = FileUtils.convertUnixPath(lookupPattern)
                    .replace("/", "\\/")
                    .replace(".", "\\.")
                    .replace("*", ".*")
                    .replace("?", ".")

            // Create pattern walker
            def walker = new PatternWalker(
                    root,
                    regexPattern.compile("^${includePatternStr}\$".toString()),
                    excludePatterns
            )

            if (recursive)
                root.eachFileRecurse(FileType.FILES, walker.&walk)
            else
                root.eachFile(FileType.FILES, walker.&walk)

            return walker.included
        }
    }

    private static class PatternWalker {

        private File root
        private regexPattern includePattern
        private List<regexPattern> excludePatterns

        final List<File> included = []

        PatternWalker(
                File root,
                regexPattern includePattern,
                List<regexPattern> excludePatterns) {
            this.root = root
            this.includePattern = includePattern
            this.excludePatterns = excludePatterns
        }

        void walk(File found) {
            // Make sure path is using the *nix slash for folders
            def relativizedFile = FileUtils.convertUnixPath(root.relativePath(found))

            // Check if file should be excluded
            if (!excludePatterns.isEmpty()) {
                // Any file match any exclusion pattern
                if (excludePatterns.any { relativizedFile ==~ /.*${it}.*/ })
                    return
            }

            // Check if file should be included
            if (!includePattern.matcher(relativizedFile).matches())
                return

            included.add(found)
        }

    }



}
