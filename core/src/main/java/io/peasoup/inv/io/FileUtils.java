package io.peasoup.inv.io;

import org.apache.commons.lang.StringUtils;

public class FileUtils {

    public final static String SCRIPT_GROOVY_TYPE = "text/x-groovy";
    public final static String SCRIPT_YAML_TYPE = "text/x-yaml";

    private FileUtils() {
        // private ctor
    }

    /**
     * Add an ending slash to a path
     * @param path Path
     * @return New instance ending with a slash
     */
    public static String addEndingSlash(String path) {
        if (StringUtils.isEmpty(path))
            throw new IllegalArgumentException("path");

        if (path.charAt(path.length() - 1) == '/') return path;

        return path + "/";
    }

    /**
     * Convert path to Unix-like path
     * @param path Path to convert
     * @return New instance with converted path
     */
    public static String convertUnixPath(String path) {
        if (StringUtils.isEmpty(path))
            throw new IllegalArgumentException("path");

        return path.replace("\\", "/");
    }

    /**
     * Get the mime type based on a filename
     * @param filename Filename
     * @return Valid mime type name
     */
    public static String getMimeType(String filename) {
        if (StringUtils.isEmpty(filename))
            throw new IllegalArgumentException("filename");

        // if not extension defined, use groovy mime
        if (!filename.contains("."))
            return SCRIPT_GROOVY_TYPE;

        // Get mime from file extension
        switch(filename.split("\\.")[1]) {
            case "groovy": return SCRIPT_GROOVY_TYPE;
            case "yml":
            case "yaml":
                return SCRIPT_YAML_TYPE;
            default: return "";
        }
    }

}
