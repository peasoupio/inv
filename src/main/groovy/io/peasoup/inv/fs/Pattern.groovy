package io.peasoup.inv.fs

import groovy.io.FileType

class Pattern {

    private Pattern() {

    }

    static List<File> get(List<String> patterns, String exclude = '', File root = null) {
        assert exclude != null, 'ExcludePattern is required. Can be empty'

        def excludePattern = exclude
                .replace("\\", "/")
                .replace("/", "\\/")
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".*")

        return patterns.collectMany {
            String lookupPattern = it

            // If pattern means an actual file, use it
            File lookupFile = new File(lookupPattern)
            if (!lookupFile.isDirectory() && lookupFile.exists())
                return [lookupFile]

            // Can't process Ant-compliant patterns without a root
            if (!root)
                return []

            // Convert Ant pattern to regex
            def includePattern = lookupPattern
                    .replace("\\", "/")
                    .replace("/", "\\/")
                    .replace(".", "\\.")
                    .replace("*", ".*")
                    .replace("?", ".*")

            List<File> included = []
            root.eachFileRecurse(FileType.FILES) {

                // Make sure path is using the *nix slash for folders
                def file = it.path.replace("\\", "/")

                // Check if file should be excluded
                if (excludePattern && file ==~ /.*${excludePattern}.*/)
                    return

                // Check if file should be included
                if (!(file ==~ /.*${includePattern}.*/))
                    return

                included << it
            }

            return included
        }
    }
}
