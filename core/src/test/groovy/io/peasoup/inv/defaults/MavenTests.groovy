package io.peasoup.inv.defaults

import io.peasoup.inv.TempHome
import io.peasoup.inv.run.InvExecutor
import io.peasoup.inv.run.InvHandler
import io.peasoup.inv.run.Logger
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TempHome.class)
class MavenTests {

    @Before
    void setup() {
        Logger.capture(null)
    }

    @Test
    void mavenSimpleLookup() {

        // Enable capture
        def logs = Logger.capture(new LinkedList())

        def app1 = new File(FilesTests.class.getResource("/defaults/maven/SimpleMavenLookup/app1").path).absolutePath
        def app2 = new File(FilesTests.class.getResource("/defaults/maven/SimpleMavenLookup/app2").path).absolutePath

        def executor = new InvExecutor()
        executor.read(new File("./defaults/files/inv.groovy"))
        executor.read(new File("./defaults/maven/inv.groovy"))

        new InvHandler(executor).call {

            name "app1"
            path app1

            // Using default
            require $inv.Maven into '$maven'

            step {
                assert $maven.poms
            }
        }

        new InvHandler(executor).call {

            name "app2"

            require $inv.Maven using {

                // Disabling defaults and calling manually
                defaults false

                resolved {
                    response.analyze(app2)
                }
            }
        }

        def report = executor.execute()

        report.errors.each {
            it.throwable.printStackTrace()
        }
        assert report.isOk()

        def flattenLogs = logs.join()

        assert flattenLogs.contains("[app1] => [BROADCAST] [Artifact] com.mycompany.app:my-app-1")
        assert flattenLogs.contains("[app2] => [BROADCAST] [Artifact] com.mycompany.app:my-app-2")
        assert flattenLogs.contains("[app2] => [REQUIRE] [Artifact] com.mycompany.app:my-app-1")
    }
}
