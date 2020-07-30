package io.peasoup.inv.composer

import io.peasoup.inv.TempHome
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.jupiter.api.Assertions.assertThrows

@RunWith(TempHome.class)
class SettingsTest {

    @Test
    void ok_alreadyExists() {
        def stageValue = "value1"
        def settingsFile = new File("./target/test-classes/settings.json")
        settingsFile.delete()


        def settings = new Settings(settingsFile)
        assert !settings.stagedIds().contains(stageValue)

        settings.stageId(stageValue)
        settings.save()


        settings = new Settings(settingsFile)
        assert settings.stagedIds().contains(stageValue)

        settingsFile
    }

    @Test
    void not_ok() {
        assertThrows(AssertionError.class, {
            new Settings(null)
        })
    }
}
