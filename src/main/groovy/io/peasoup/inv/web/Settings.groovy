package io.peasoup.inv.web

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class Settings {

    final private Map settings = [
        filters: [
            defaultStep: 20
        ],
        staged: []
    ]
    final private File settingsFile

    Settings(File settingsFile) {
        this.settingsFile = settingsFile

        if (settingsFile.exists())
            settings += new JsonSlurper().parse(settingsFile.newReader())
    }


    Map filters() {
        return settings.filters
    }

    void stage(String id) {
        if (settings.staged.contains(id))
            return

        settings.staged << id
    }

    void unstage(String id) {
        if (!settings.staged.contains(id))
            return

        settings.staged.remove(id)
    }

    void unstageAll() {
        settings.staged.clear()
    }


    List<String> staged() {
        return settings.staged
    }


    void save() {
        settingsFile.write(JsonOutput.prettyPrint(JsonOutput.toJson(settings)))
    }

}
