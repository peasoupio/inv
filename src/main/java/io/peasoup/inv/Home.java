package io.peasoup.inv;

import java.io.File;

public class Home {

    public static final File DEFAULT_HOME = new File("./");
    public static File current;

    static {
        current = DEFAULT_HOME;
    }
}
