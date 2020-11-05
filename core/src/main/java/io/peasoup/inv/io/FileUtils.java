package io.peasoup.inv.io;

import org.apache.commons.lang.StringUtils;

public class FileUtils {

    private FileUtils() {
        // private ctor
    }

    public static String addSubordinateSlash(String path) {
        assert StringUtils.isNotEmpty(path);

        if (path.charAt(path.length() - 1) == '/') return path;

        return path + "/";
    }

    public static String convertUnixPath(String path) {
        return path.replace("\\", "/");
    }
}
