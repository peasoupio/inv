package io.peasoup.inv.loader;

import java.io.File;
import java.util.LinkedList;

/**
 * Expected file structure :
 * some-path/
 *     .inv/
 *         resources/
 *         src/
 *             // (many) Groovy class files
 *             MyClass.groovy
 *         test/
 *              // (many) INV junit test files
 *             test.groovy
 *         vars/
 *             // (many) INV definition files. Could be a YAMl file.
 *             inv.groovy
 *
 *          // (single) REPO file. Could be a YAMl file.
 *          repo.groovy
 */
public class PackageDiscoverer {

    private PackageDiscoverer() {
        // ctor
    }

    /**
     * Gets full package name for the specified file.
     * @param lookupFile Lookup file
     * @return String representation of the full package name, otherwise null
     */
    public static String forFile(File lookupFile) {
        if (lookupFile == null) return null;

        String filename = lookupFile.getName().toLowerCase();
        switch (filename) {
            case "inv.groovy":
            case "inv.yml":
            case "inv.yaml":
            case "repo.groovy":
            case "repo.yaml":
            case "repo.yml":
                return resolveVars(lookupFile);
            default:
                return resolveSrc(lookupFile);
        }
    }


    /**
     * Make sure lookup file is inside a .inv/vars folder
     * @param lookupFile Lookup file
     * @return
     */
    private static String resolveVars(File lookupFile) {
        if (lookupFile.getParentFile() == null) return null;

        File lookupFileParent = lookupFile.getParentFile();
        if (!lookupFileParent.getName().equalsIgnoreCase("vars")) return null;
        if (lookupFileParent.getParentFile() == null) return null;
        if (!lookupFileParent.getParentFile().getName().equalsIgnoreCase(".inv")) return null;

        return resolvePath(lookupFileParent.getParentFile().getParentFile());
    }

    private static String resolveSrc(File lookupFile) {
        if (lookupFile.getParentFile() == null) return null;

        File currentFile = lookupFile.getParentFile();
        while(currentFile != null) {
            if (currentFile.getName().equalsIgnoreCase("src") &&
                currentFile.getParentFile() != null &&
                currentFile.getParentFile().getName().equalsIgnoreCase(".inv"))
                return resolvePath(currentFile.getParentFile().getParentFile());

            currentFile = currentFile.getParentFile();
        }

        // No match found for ./inv/src
        return null;
    }

    private static String resolvePath(File lookupFile) {
        if (lookupFile == null) return null;

        LinkedList<String> packages = new LinkedList<>();
        File currentFile = lookupFile;
        while(currentFile.getParentFile() != null) {
            packages.offerFirst(currentFile.getName());
            currentFile = currentFile.getParentFile();
        }
        // Add root filename
        packages.offerFirst(currentFile.getName());

        return String.join(".", packages);
    }

}
