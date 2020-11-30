package io.peasoup.inv.composer

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class Settings {

    private volatile Map settings = [
            filters: [
                    defaultStep: 20
            ],
            staged : [
                    ids : [],
                    repos: []
            ]
    ]
    final private File settingsFile

    /**
     * Create a new Settings
     * @param settingsFile Settings file is required, but can NOT exist. It will be created automatically
     */
    Settings(File settingsFile) {
        if (settingsFile == null)
            throw new IllegalArgumentException("settingsFile")

        this.settingsFile = settingsFile

        if (settingsFile.exists())
            apply(new JsonSlurper().parse(settingsFile.newReader()) as Map)
    }

    Map filters() {
        return settings.filters as Map
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

    void stageREPO(String id) {
        settings.staged.repos << id
    }

    void unstageREPO(String id) {
        settings.staged.repos.remove(id)
    }

    void unstageAllREPOs() {
        settings.staged.repos.clear()
    }

    List<String> stagedREPOs() {
        return settings.staged.repos
    }

    synchronized void save() {
        settingsFile.write(toString())
    }

    void apply(Map values) {
        settings += values
    }

    @Override
    String toString() {
        return JsonOutput.prettyPrint(JsonOutput.toJson(settings))
    }
}
