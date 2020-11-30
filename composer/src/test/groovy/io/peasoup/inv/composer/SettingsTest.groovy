package io.peasoup.inv.composer

import io.peasoup.inv.TempHome
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(TempHome.class)
class SettingsTest {

    @Test
    void ok_alreadyExists() {
        def stageValue = "value1"
        def settingsFile = new File(SettingsTest.getResource("/settings.json").path)
        settingsFile.delete()


        def settings = new Settings(settingsFile)
        assertFalse settings.stagedIds().contains(stageValue)

        settings.stageId(stageValue)
        settings.save()


        settings = new Settings(settingsFile)
        assertTrue settings.stagedIds().contains(stageValue)

        settingsFile
    }

    @Test
    void not_ok() {
        assertThrows(IllegalArgumentException.class, {
            new Settings(null)
        })
    }
}
