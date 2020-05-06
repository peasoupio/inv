package io.peasoup.inv

import groovy.json.JsonOutput
import io.peasoup.inv.cli.ScmCommand
import io.peasoup.inv.run.Logger
import io.peasoup.inv.scm.ScmExecutor
import io.peasoup.inv.utils.Stdout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TempHome.class)
class MainTest {

    @Before
    void before() {
        Main.embedded = true
    }

    @After
    void after() {
        Logger.capture(null)

        Main.embedded = false
    }

    @Test
    void main() {
        // Enable capture
        def logs = Logger.capture(new LinkedList())

        def script = MainTest.class.getResource("/mainTestScript.groovy")
        assert script

        def canonicalPath = new File(script.path).canonicalPath

        Main.main("run", "-x", script.path)

        assert Main.exitCode == 0
        assert logs.contains("[undefined] [${canonicalPath}] [mainTestScript]".toString())
    }

    @Test
    void main_secure() {
        // Enable capture
        def logs = Logger.capture(new LinkedList())

        def script = MainTest.class.getResource("/mainTestScript.groovy")
        assert script

        def canonicalPath = new File(script.path).canonicalPath

        Main.main("run", "-s", script.path)

        assert Main.exitCode == 0
        assert logs.contains("[undefined] [${canonicalPath}] [mainTestScript]".toString())
    }

    @Test
    void main_no_args() {

        Stdout.capture ({ Main.main() }, {
            assert Main.exitCode == -1
            assert it.contains("Usage")
        })
    }

    @Test
    void main_with_expansion() {
        // Enable capture
        def logs = Logger.capture(new LinkedList())

        def files = [
            new File(TempHome.testResources, "/mainTestScript.groovy").canonicalPath,
            new File(TempHome.testResources, "/mainTestScript2.groovy").canonicalPath
        ]

        Main.main("run", "-e", "pattern", "test-resources/mainTestScript.groovy", "test-resources/mainTestScript2.groovy")

        assert Main.exitCode == 0
        assert logs.contains("[undefined] [${files[0]}] [mainTestScript]".toString())
        assert logs.contains("[undefined] [${files[1]}] [mainTestScript2]".toString())
    }

    @Test
    void main_with_pattern() {
        // Enable capture
        def logs = Logger.capture(new LinkedList())

        def files = [
                new File(TempHome.testResources, "/mainTestScript.groovy").canonicalPath,
                new File(TempHome.testResources, "/mainTestScript2.groovy").canonicalPath
        ]

        Main.main("run", "test-resources/mainTestScript*.*")

        assert Main.exitCode == 0
        assert logs.contains("[undefined] [${files[0]}] [mainTestScript]".toString())
        assert logs.contains("[undefined] [${files[1]}] [mainTestScript2]".toString())

    }

    @Test
    void main_with_pattern_2() {

        // Enable capture
        def logs = Logger.capture(new LinkedList())

        def files = [
            new File(TempHome.testResources, "/pattern/inside/folder/mainTestScript.groovy").canonicalPath,
            new File(TempHome.testResources, "/pattern/inside/different/mainTestScript2.groovy").canonicalPath,
        ]

        Main.main("run", "test-resources/pattern/**/*.*")

        assert Main.exitCode == 0
        assert logs.contains("[undefined] [${files[0]}] [different-folder]".toString())
        assert logs.contains("[undefined] [${files[1]}] [different-inside]".toString())

    }

    @Test
    void main_graph() {
        def logOutput = MainTest.class.getResource("/baseRun.txt")

        assert logOutput

        println "\nTest selecting 'plain': "
        Main.main("graph", "plain", logOutput.path)

        assert Main.exitCode == 0

        println "\nTest selecting 'dot': "
        Main.main("graph", "dot", logOutput.path)

        assert Main.exitCode == 0
    }

    @Test
    void main_launchScm() {
        def scmFile = new File(TempHome.testResources, '/scm.groovy')

        def comparable = new ScmExecutor()
        comparable.read(scmFile)

        assert comparable.scms["my-repository"]

        // Remove to make sure we trigger init
        if (comparable.scms["my-repository"].path.exists())
            comparable.scms["my-repository"].path.deleteDir()

        Stdout.capture ({ Main.main("scm", scmFile.path) }, {
            assert Main.exitCode == 0
            assert it.contains("init")
        })

        Stdout.capture ({ Main.main("scm", scmFile.path) }, {
            assert Main.exitCode == 0
            assert it.contains("update")
        })
    }

    @Test
    void main_launchScm_relative() {
        def logs = Logger.capture(new LinkedList())

        def scmFile = new File(TempHome.testResources, '/scm-relative.groovy')

        def comparable = new ScmExecutor()
        comparable.read(scmFile)

        assert comparable.scms["my-repository-relative"]
        def scriptFile = new File(comparable.scms["my-repository-relative"].path, comparable.scms["my-repository-relative"].entry[0])
        assert scriptFile.exists()

        Main.main("scm", scmFile.path)

        assert logs.contains("[my-repository-relative] [${scriptFile.canonicalPath}] [mainTestScript]".toString())
    }

    @Test
    void main_launchScm_list() {
        def logs = Logger.capture(new LinkedList())

        def scm1 = new File(TempHome.testResources, '/scm.groovy')
        def scm2 = new File(TempHome.testResources, '/scm-relative.groovy')

        def comparable = new ScmExecutor()
        comparable.read(scm1)
        comparable.read(scm2)

        def scmListFile = new File(TempHome.testResources, ScmCommand.LIST_FILE_SUFFIX)
        scmListFile << JsonOutput.toJson([
                "scm1": [ script: scm1.absolutePath],
                "scm2": [ script: scm2.absolutePath]
        ])

        Main.main("scm", scmListFile.path)

        def scm1Entry = new File(comparable.scms["my-repository"].entry[0])
        def scm2Entry = new File(comparable.scms["my-repository-relative"].path, comparable.scms["my-repository-relative"].entry[0])

        assert logs.contains("[my-repository] [${scm1Entry.canonicalPath}] [mainTestScript]".toString())
        assert logs.contains("[my-repository-relative] [${scm2Entry.canonicalPath}] [mainTestScript]".toString())
    }

    @Test
    void main_syntax_ok() {

        def validSyntaxFile = MainTest.class.getResource("/syntax-valid.groovy")
        def invalidSyntaxFile = MainTest.class.getResource("/syntax-invalid.groovy")

        assert validSyntaxFile
        assert invalidSyntaxFile

        Main.main("syntax", validSyntaxFile.path)
        assert Main.exitCode == 0

        Main.main("syntax", invalidSyntaxFile.path)
        assert Main.exitCode == -2
    }

    @Test
    void main_delta() {

        def logOutput = MainTest.class.getResource("/baseRun.txt")
        def logOutputAfter = MainTest.class.getResource("/subsetRun.txt")

        assert logOutput
        assert logOutputAfter

        Main.main("delta", logOutput.path, logOutputAfter.path)

        assert Main.exitCode == 0
    }
}