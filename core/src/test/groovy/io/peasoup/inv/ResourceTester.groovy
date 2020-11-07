package io.peasoup.inv

class ResourceTester {

    private final String resourceDir

    ResourceTester(String resourceDir) {
        this.resourceDir = resourceDir
    }

    String interpolate(String value) {
        if (value.startsWith("file:/"))
            value = value.replace("file:/", resourceDir)

        if (value.startsWith("url:/"))
            value = value.replace("url:/", "https://raw.githubusercontent.com/")

        return value
    }
}
