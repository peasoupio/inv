package io.peasoup.inv;

import java.io.File;

public class Home {

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

    private Home() {

    }
}
