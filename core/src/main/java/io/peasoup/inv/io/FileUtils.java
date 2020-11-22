package io.peasoup.inv.io;

import org.apache.commons.lang.StringUtils;

public class FileUtils {

    private FileUtils() {
        // private ctor
    }

    public static String addSubordinateSlash(String path) {
        if (StringUtils.isEmpty(path))
            throw new IllegalArgumentException("path");

        if (path.charAt(path.length() - 1) == '/') return path;

        return path + "/";
    }

    public static String convertUnixPath(String path) {
        if (StringUtils.isEmpty(path))
            throw new IllegalArgumentException("path");

        return path.replace("\\", "/");
    }
}
