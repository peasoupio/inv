package io.peasoup.inv.composer

import groovy.json.JsonOutput
import io.peasoup.inv.composer.utils.MapUtils
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

class Settings {

    final static DEFAULT_SETTINGS_NAME = "settings.yml"

    private Map settings = [
            filters: [
                    defaultStep: 20
            ],
            staged : [
                    ids : [],
                    repos: []
            ],
            security: [
                    enabled: false
            ]
    ]

    private final File settingsFile

    /**
     * Create a new Settings
     * @param settingsFile Settings file is required, but can NOT exist. It will be created automatically
     */
    Settings(File settingsFile) {
        if (settingsFile == null)
            throw new IllegalArgumentException("settingsFile")

        this.settingsFile = settingsFile

        if (settingsFile.exists())
            apply(load())
        else
            save()
    }

    Map filters() {
        return settings.filters as Map
    }

    // Security

    boolean isSecurityEnabled() {
        return settings.security?.enabled
    }


    // IDs (INVS)

    void stageId(String id) {
        settings.staged.ids << id
    }

    List<String> stagedIds() {
        return settings.staged.ids
    }

    void unstageId(String id) {
        settings.staged.ids.remove(id)
    }

    void unstageAllIds() {
        settings.staged.ids.clear()
    }


    // Repos

    void stageREPO(String id) {
        settings.staged.repos << id
    }

    List<String> stagedREPOs() {
        return settings.staged.repos
    }

    void unstageREPO(String id) {
        settings.staged.repos.remove(id)
    }

    void unstageAllREPOs() {
        settings.staged.repos.clear()
    }

    /**
     * Load the current settings as a Map object
     * @return Map representation of the YAML settings file
     */
    synchronized Map load() {
        new Yaml().load(settingsFile.newReader())
    }

    /**
     * Save current settings to the filesystem.
     */
    synchronized void save() {
        settingsFile.write(toString())
    }

    /**
     * Apply new (or update existing) values to settings
     * @param values Values to apply
     */
    void apply(Map values) {
        MapUtils.merge(settings, values)
    }

    String toYaml() {
        StringWriter sw = new StringWriter()

        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        def yaml = new Yaml(options)
        yaml.dump(settings, sw)

        return sw.toString()
    }

    String toJson() {
        return JsonOutput.toJson(settings)
    }

    @Override
    String toString() {
        return toYaml()
    }
}
