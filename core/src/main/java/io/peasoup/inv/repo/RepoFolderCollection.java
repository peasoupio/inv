package io.peasoup.inv.repo;

import io.peasoup.inv.Logger;
import io.peasoup.inv.io.FileUtils;
import io.peasoup.inv.loader.FgroupLoader;
import io.peasoup.inv.run.InvExecutor;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class RepoFolderCollection {

    private final RepoExecutor repoExecutor = new RepoExecutor();
    private final Map<RepoDescriptor, FgroupLoader.RepoMatches> matchesCache = new HashMap<>();

    private final InvExecutor invExecutor;

    public RepoFolderCollection(InvExecutor invExecutor) {
        if (invExecutor == null) throw new IllegalArgumentException("invExecutor");

        this.invExecutor = invExecutor;
    }

    /**
     * Add a REPO file location.
     * It can be a File or Directory type.
     * In case of a directory type, it will expect a REPO folder structure (vars, src, test, etc..)
     *
     * @param repoFileLocation A valid non-empty REPO file location on the current file system
     * @return True if added, otherwise false
     */
    public boolean add(String repoFileLocation) {
        return this.add(repoFileLocation, RepoInvoker.expectedParametersfileLocation(repoFileLocation));
    }

    /**
     * Add a REPO file location.
     * It can be a File or Directory type.
     * In case of a directory type, it will expect a REPO folder structure (vars, src, test, etc..)
     *
     * @param repoFileLocation A valid non-empty REPO file location on the current file system
     * @param expectedParameterFile Expected parameter file location
     * @return True if added, otherwise false
     */
    public boolean add(String repoFileLocation, String expectedParameterFile) {
        if (StringUtils.isEmpty(repoFileLocation))
            throw new IllegalArgumentException("repoFileLocation");

        // If a single file, assume its an repo file
        if (new File(repoFileLocation).isFile()) {
            // Parse the actual repo file
            parseRepofile(repoExecutor, repoFileLocation, expectedParameterFile);

            return true;
        } else {
            // Otherwise, expect a repo folder
            return this.add(FgroupLoader.findMatches(repoFileLocation), expectedParameterFile);
        }
    }

    /**
     * Add a REPO fgroup file matches.
     * It expects a valid REPO folder structure.
     *
     * @param matches A valid fgroup REPO file matches
     * @param expectedParameterFile Expected parameter file location
     * @return True if added, otherwise false
     */
    public boolean add(FgroupLoader.RepoMatches matches, String expectedParameterFile) {
        if (matches == null)
            throw new IllegalArgumentException("matches");

        if (matches.getRepoFile() == null)
            return false;

        // Parse the actual repo file
        String repoFileLocation = matches.getRepoFile().toString();
        RepoExecutor singleUseRepoExecutor = new RepoExecutor();

        parseRepofile(singleUseRepoExecutor, repoFileLocation, expectedParameterFile);

        // Gets the resolved descriptors and add them to the cache
        for (RepoDescriptor repoDescriptor: singleUseRepoExecutor.getRepos().values()) {
            this.matchesCache.put(repoDescriptor, matches);
        }

        return true;
    }

    /**
     * Read added repos and load invs into the executor
     */
    public boolean bulkRead() {
        // Execute repos descriptors
        for(RepoExecutor.RepoHookExecutionReport report : repoExecutor.execute()) {

            // If repo has not been processed, do not proceed with it
            if (!report.isProcessed())
                continue;

            // If report is not ok, stop everything
            if (!report.isOk())
                return false;

            // Get matches for
            FgroupLoader.RepoMatches matches = FgroupLoader.findMatches(report.getDescriptor().getRepoCompletePath().getAbsolutePath());

            this.matchesCache.put(report.getDescriptor(), matches);
        }

        // For every matching repo descriptor and invmatches, parse its REPO file
        for(Map.Entry<RepoDescriptor, FgroupLoader.RepoMatches> kpv : this.matchesCache.entrySet()) {
            parseRepoFolderFiles(kpv.getKey(), kpv.getValue());
        }
        this.matchesCache.clear();

        return true;
    }

    private void parseRepofile(RepoExecutor currentRepoExecutor, String repoFileLocation, String expectedParametersFileLocation) {
        File localRepofile = new File(repoFileLocation);
        File expectedParametersFile = RepoInvoker.expectedParametersfileLocation(localRepofile);

        if (StringUtils.isNotEmpty(expectedParametersFileLocation))
            expectedParametersFile = new File(expectedParametersFileLocation);

        currentRepoExecutor.addScript(
                localRepofile,
                expectedParametersFile);
    }

    private void parseRepoFolderFiles(RepoDescriptor descriptor, FgroupLoader.RepoMatches matches) {
        String path = matches.getRootPath();
        String name = descriptor.getName();
        String packageName = name;

        // Invoke groovy files
        invokeGroovyfiles(matches, packageName);

        // Invoke inv files
        invokeInvfiles(matches, packageName, path, name);
    }

    private void invokeGroovyfiles(FgroupLoader.RepoMatches matches, String packageName) {
        // Parse groovy classes
        for(Path groovyFile : matches.getGroovyFiles()) {

            if (Logger.isDebugEnabled())
                Logger.debug("[REPO] [CLASS] path: " + FileUtils.convertUnixPath(Path.of(matches.getRootPath()).relativize(groovyFile).toString()) + ", package: " + packageName);

            invExecutor.addClass(groovyFile.toFile(), packageName);
        }
    }

    private void invokeInvfiles(FgroupLoader.RepoMatches matches, String packageName, String path, String repo) {
        // Parse inv files.
        for(Path invFile : matches.getInvFiles()) {

            invExecutor.addScript(
                    invFile.toFile(),
                    packageName,
                    path,
                    repo);
        }
    }
}
