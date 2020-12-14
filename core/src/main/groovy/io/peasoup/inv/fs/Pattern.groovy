package io.peasoup.inv.fs

import groovy.io.FileType
import io.peasoup.inv.io.FileUtils

class Pattern {

    private Pattern() {

    }

    static List<File> get(List<String> patterns, List<String> excludes = [], File root = null, boolean recursive = true) {
        assert patterns != null, 'Patterns is required.'
        assert excludes != null, 'Exclude is required. Can be empty'

        def excludePatterns = excludes.collect {
            FileUtils.convertUnixPath(it)
                    .replace("/", "\\/")
                    .replace(".", "\\.")
                    .replace("*", ".*")
                    .replace("?", ".*")
        }

        return patterns.collectMany {
            String lookupPattern = it

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
                    .replace("?", ".*")

            // Create pattern
            def includePattern = java.util.regex.Pattern.compile("^${includePatternStr}\$".toString())

            List<File> included = []
            def walker = { File found ->
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

            if (recursive)
                root.eachFileRecurse(FileType.FILES, walker)
            else
                root.eachFile(FileType.FILES, walker)

            return included
        }
    }
}
