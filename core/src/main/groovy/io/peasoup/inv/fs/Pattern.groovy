package io.peasoup.inv.fs

import groovy.io.FileType

class Pattern {

    private Pattern() {

    }

    static List<File> get(List<String> patterns, List<String> excludes = [], File root = null, boolean recursive = true) {
        assert patterns != null, 'Patterns is required.'
        assert excludes != null, 'Exclude is required. Can be empty'

        def excludePatterns = excludes.collect {
            it.replace("\\", "/")
                    .replace("/", "\\/")
                    .replace(".", "\\.")
                    .replace("*", ".*")
                    .replace("?", ".*")
        }

        return patterns.collectMany {
            String lookupPattern = it

            // If pattern means an actual file, use it
            File lookupFile = new File(lookupPattern)
            if (!lookupFile.isDirectory() && lookupFile.exists())
                return [lookupFile]

            // Can't process Ant-compliant patterns without a root
            if (!root)
                return []

            // If patterns mean an actual file the root, use it
            File lookupFileRoot = new File(root, lookupPattern)
            if (!lookupFileRoot.isDirectory() && lookupFileRoot.exists())
                return [lookupFileRoot]

            // Convert Ant pattern to regex
            def includePattern = lookupPattern
                    .replace("\\", "/")
                    .replace("/", "\\/")
                    .replace(".", "\\.")
                    .replace("*", ".*")
                    .replace("?", ".*")

            List<File> included = []
            def walker = { File found ->
                // Make sure path is using the *nix slash for folders
                def relativizedFile = root.relativePath(found)
                        .replace("\\", "/")

                // Check if file should be excluded
                if (excludePatterns) {
                    // Any file match any exclusion pattern
                    if (excludePatterns.any { relativizedFile ==~ /.*${it}.*/ })
                        return
                }

                // Check if file should be included
                if (!(relativizedFile ==~ /^${includePattern}$/))
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
