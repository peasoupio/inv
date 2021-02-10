package io.peasoup.inv.testing;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import io.peasoup.inv.run.InvDescriptor;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SimulatorDescriptor {

    private boolean includeSrcFiles = true;

    private final List<String> invFiles;
    private final List<String> repoFiles;
    private final List<String> repoUrls;
    private final List<Closure<?>> invBodies;

    public SimulatorDescriptor() {
        this.invFiles = new ArrayList<>();
        this.repoFiles = new ArrayList<>();
        this.repoUrls = new ArrayList<>();
        this.invBodies = new ArrayList<>();
    }

    /**
     * Set whether or not source files located at /src are loaded.
     * By default, value is true.
     *
     * @param value True to load, otherwise false.
     */
    public void includeSrcFiles(boolean value) {
        this.includeSrcFiles = value;
    }


    /**
     * Adds a INV file location to the simulation.
     *
     * @param invLocation A non-empty valid INV file location on the current filesystem.
     */
    public void addInvFile(String invLocation) {
        if (StringUtils.isEmpty(invLocation))
            throw new IllegalArgumentException("invLocation");

        this.invFiles.add(invLocation);
    }

    /**
     * Adds a REPO file location to the simulation.
     * The REPO file can be a single file or a REPO folder.
     *
     * @param repoLocation A non-empty valid REPO file location on the current filesystem.
     */
    public void addRepoFile(String repoLocation) {
        if (StringUtils.isEmpty(repoLocation))
            throw new IllegalArgumentException("repoLocation");

        this.repoFiles.add(repoLocation);
    }

    /**
     * Adds a REPO url location to the simulation
     *
     * @param repoURL A non-empty valid REPO url location
     */
    public void addRepoUrl(String repoURL) {
        if (StringUtils.isEmpty(repoURL))
            throw new IllegalArgumentException("repoURL");

        this.repoUrls.add(repoURL);
    }

    /**
     * Adds an arbitrary INV body descriptor to the simulation.
     * Usually, those bodies are made for the acutal testing and assertions.
     *
     * @param body A non-null valid INV body descriptor.
     */
    public void addInvBody(@DelegatesTo(InvDescriptor.class) Closure<?> body) {
        if (body == null)
            throw new IllegalArgumentException("body");

        this.invBodies.add(body);
    }

    /**
     * Determines if there is something to do with this simulation.
     *
     * @return True if something to do, otherwise false.
     */
    public boolean hasSomethingToDo() {
        return !this.invFiles.isEmpty() ||
               !this.repoFiles.isEmpty() ||
               !this.repoUrls.isEmpty() ||
               !this.invBodies.isEmpty();
    }
}
