package io.peasoup.inv

import groovy.json.JsonOutput
import io.peasoup.inv.cli.RepoRunCommand
import io.peasoup.inv.repo.RepoExecutor
import io.peasoup.inv.run.Logger
import io.peasoup.inv.utils.Stdout
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TempHome.class)
class MainTest {

    @Test
    void start() {
        // Enable capture
        def logs = Logger.capture(new LinkedList())

        def script = MainTest.class.getResource("/mainTestScript.groovy")
        assert script

        def canonicalPath = new File(script.path).canonicalPath

        Main.start("run", "-x", script.path)

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

        Main.start("run", "-s", script.path)

        assert Main.exitCode == 0
        assert logs.contains("[undefined] [${canonicalPath}] [mainTestScript]".toString())
    }

    @Test
    void main_no_args() {

        Stdout.capture ({ Main.start() }, {
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

        Main.start("run", "-e", "pattern", "test-resources/mainTestScript.groovy", "test-resources/mainTestScript2.groovy")

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

        Main.start("run", "test-resources/mainTestScript*.*")

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

        Main.start("run", "test-resources/pattern/**/*.*")

        assert Main.exitCode == 0
        assert logs.contains("[undefined] [${files[0]}] [different-folder]".toString())
        assert logs.contains("[undefined] [${files[1]}] [different-inside]".toString())

    }

    @Test
    void main_graph() {
        def logOutput = MainTest.class.getResource("/baseRun.txt")

        assert logOutput

        println "\nTest selecting 'plain': "
        Main.start("graph", "plain", logOutput.path)

        assert Main.exitCode == 0

        println "\nTest selecting 'dot': "
        Main.start("graph", "dot", logOutput.path)

        assert Main.exitCode == 0
    }

    @Test
    void main_launchRepo() {
        def repoFile = new File(TempHome.testResources, '/repo.groovy')

        def comparable = new RepoExecutor()
        comparable.parse(repoFile)

        assert comparable.repos["my-repository"]

        // Remove to make sure we trigger init
        if (comparable.repos["my-repository"].path.exists())
            comparable.repos["my-repository"].path.deleteDir()

        Stdout.capture ({ Main.start("repo", "run", repoFile.path) }, {
            assert Main.exitCode == 0
            assert it.contains("init")
        })

        Stdout.capture ({ Main.start("repo", "run", repoFile.path) }, {
            assert Main.exitCode == 0
            assert it.contains("pull")
        })
    }

    @Test
    void main_launchRepo_list() {
        def logs = Logger.capture(new LinkedList())

        def repo1 = new File(TempHome.testResources, '/repo.groovy')
        def repo2 = new File(TempHome.testResources, '/repo-relative.groovy')

        def comparable = new RepoExecutor()
        comparable.parse(repo1)
        comparable.parse(repo2)

        def repoListFile = new File(TempHome.testResources, RepoRunCommand.LIST_FILE_SUFFIX)
        repoListFile << JsonOutput.toJson([
                "repo1": [ script: repo1.absolutePath],
                "repo2": [ script: repo2.absolutePath]
        ])

        Main.start("repo", "run", repoListFile.path)

        def repo1Entry = new File(comparable.repos["my-repository"].entry[0])
        def repo2Entry = new File(comparable.repos["my-repository-relative"].path, comparable.repos["my-repository-relative"].entry[0])

        assert logs.contains("[my-repository] [${repo1Entry.canonicalPath}] [mainTestScript]".toString())
        assert logs.contains("[my-repository-relative] [${repo2Entry.canonicalPath}] [mainTestScript]".toString())
    }

    @Test
    void main_syntax_ok() {

        def validSyntaxFile = MainTest.class.getResource("/syntax-valid.groovy")
        def invalidSyntaxFile = MainTest.class.getResource("/syntax-invalid.groovy")

        assert validSyntaxFile
        assert invalidSyntaxFile

        Main.start("syntax", validSyntaxFile.path)
        assert Main.exitCode == 0

        Main.start("syntax", invalidSyntaxFile.path)
        assert Main.exitCode == -2
    }

    @Test
    void main_delta() {

        def logOutput = MainTest.class.getResource("/baseRun.txt")
        def logOutputAfter = MainTest.class.getResource("/subsetRun.txt")

        assert logOutput
        assert logOutputAfter

        Main.start("delta", logOutput.path, logOutputAfter.path)

        assert Main.exitCode == 0
    }

    @Test
    void main_test() {
        def script = MainTest.class.getResource("/inv-test-script.groovy")
        assert script

        Main.start("test", "-x", script.path)

        assert Main.exitCode == 0
    }
}