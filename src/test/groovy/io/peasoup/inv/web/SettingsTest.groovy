package io.peasoup.inv.web


import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class SettingsTest {

    @Test
    void ok_alreadyExists() {
        def stageValue = "value1"
        def settingsFile = new File("./target/test-classes/settings.json")
        settingsFile.delete()


        def settings = new Settings(settingsFile)
        assert !settings.staged().contains(stageValue)

        settings.stage(stageValue)
        settings.save()


        settings = new Settings(settingsFile)
        assert settings.staged().contains(stageValue)

        settingsFile
    }

    @Test
    void not_ok() {
        assertThrows(PowerAssertionError.class, {
            new Settings(null)
        })
    }
}
