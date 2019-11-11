package io.peasoup.inv.defaults

import io.peasoup.inv.InvHandler
import io.peasoup.inv.InvInvoker
import io.peasoup.inv.Logger
import org.junit.Before
import org.junit.Test

class MavenTests {

    @Before
    void setup() {
        ExpandoMetaClass.enableGlobally()
        Logger.capture(null)
        Logger.DebugModeEnabled = true
    }

    @Test
    void mavenSimpleLookup() {

        // Enable capture
        def logs = Logger.capture([])

        def app1 = new File(FilesTests.class.getResource("/defaults/maven/SimpleMavenLookup/app1").path).absolutePath
        def app2 = new File(FilesTests.class.getResource("/defaults/maven/SimpleMavenLookup/app2").path).absolutePath

        def inv = new InvHandler()

        InvInvoker.invoke(inv, new File("./defaults/files/inv.groovy"))
        InvInvoker.invoke(inv, new File("./defaults/maven/inv.groovy"))

        inv {
            name "app1"

            require inv.SimpleMavenLookup using {
                resolved { analyze(app1) }
            }
        }

        inv {
            name "app2"

            require inv.SimpleMavenLookup using {
                resolved { analyze(app2) }
            }
        }

        inv()

        def flattenLogs = logs.join()

        assert flattenLogs.contains("[app1] => [BROADCAST] [Artifact] com.mycompany.app:my-app-1")
        assert flattenLogs.contains("[app2] => [BROADCAST] [Artifact] com.mycompany.app:my-app-2")
        assert flattenLogs.contains("[app2] => [REQUIRE] [Artifact] com.mycompany.app:my-app-1")
    }
}
