package io.peasoup.inv.loader

@Grab('commons-lang:commons-lang:2.6')
import org.apache.commons.lang.StringUtils

inv {
    // Should NOT raise exception since it has a Grab instruction
    getClass().classLoader.loadClass("org.apache.commons.lang.StringUtils")

    // Call twice to make sure it is now into the classes cache.
    getClass().classLoader.loadClass("org.apache.commons.lang.StringUtils")
}