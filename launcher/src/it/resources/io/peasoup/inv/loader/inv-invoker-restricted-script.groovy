package io.peasoup.inv.loader

inv {
    // Should raise exception since its a INV library
    getClass().classLoader.loadClass("org.apache.commons.lang.StringUtils")
}