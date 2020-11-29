package io.peasoup.inv.composer

import spark.utils.StringUtils

class URLUtils {

    private URLUtils() {
        // private ctor
    }

    static String urlify(String url) {
        if (StringUtils.isEmpty(url))
            return url

        return url.replaceAll(" ", "%20")
    }
}
