package io.peasoup.inv;

import java.io.File;

public class Home {

    public static final File DEFAULT_HOME = new File("./");

    private static File current;

    public static File getCurrent() {
        if (current == null)
            current = DEFAULT_HOME;

        return current;
    }

    public static void setCurrent(File current) {
        Home.current = current;
    }

    private Home() {

    }
}
