package io.peasoup.inv;

import java.io.File;

public class Home {

    public static final String TARGET_FOLDER_NAME = "target";
    public static final String CLASSES_FOLDER_NAME = "classes";
    public static final String REPOS_FOLDER_NAME = "repos";
    public static final String RUNS_FOLDER_NAME = "runs";

    public static final File DEFAULT_HOME = new File("./");

    private static File current;

    /**
     * Gets the current Home filesystem location
     *
     * @return File reference to the current location
     */
    public static File getCurrent() {
        if (current == null)
            current = DEFAULT_HOME;

        return current;
    }

    /**
     * Sets the current Home filesystem location
     *
     * @param current File reference to the current location
     */
    public static void setCurrent(File current) {
        Home.current = current;
    }


    /**
     * Gets the target folder where classes, run files, and repos are saved.
     * @return The target folder
     */
    public static File getTargetFolder() {
        return new File(current, TARGET_FOLDER_NAME);
    }

    /**
     * Get the classes folder where Groovy classes are compiled to
     * @return The classes folder.
     */
    public static File getClassesFolder() {
        return new File(getTargetFolder(), CLASSES_FOLDER_NAME);
    }

    /**
     * Gets the REPOS folder where REPO file and their content is extracted to.
     * @return The REPOS folder.
     */
    public static File getReposFolder() {
        return new File(getTargetFolder(), REPOS_FOLDER_NAME);
    }

    /**
     * Gets the runs folder where each runs log and other data is saved to.
     * @return The runs folder.
     */
    public static File getRunsFolder() {
        return new File(getTargetFolder(), RUNS_FOLDER_NAME);
    }

    private Home() {

    }
}
