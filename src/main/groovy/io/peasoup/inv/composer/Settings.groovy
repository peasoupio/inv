package io.peasoup.inv.composer

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class Settings {

    private Map settings = [
        filters: [
            defaultStep: 20
        ],
        staged: [
            ids: [],
            scms: []
        ]
    ]
    final private File settingsFile

    Settings(File settingsFile) {
        assert settingsFile != null, 'Settings file is required. NOTE: may NOT exist. Will be created automatically'

        this.settingsFile = settingsFile

        if (settingsFile.exists())
            settings += new JsonSlurper().parse(settingsFile.newReader())
    }

    Map filters() {
        return settings.filters
    }

    void stageId(String id) {
        settings.staged.ids << id
    }

    void unstageId(String id) {
        settings.staged.ids.remove(id)
    }

    void unstageAllIds() {
        settings.staged.ids.clear()
    }

    List<String> stagedIds() {
        return settings.staged.ids
    }

    void stageSCM(String id) {
        settings.staged.scms << id
    }

    void unstageSCM(String id) {
        settings.staged.scms.remove(id)
    }

    void unstageAllSCMs() {
        settings.staged.scms.clear()
    }

    List<String> stagedSCMs() {
        return settings.staged.scms
    }

    synchronized void save() {
        settingsFile.write(JsonOutput.prettyPrint(JsonOutput.toJson(settings)))
    }

}
