package io.peasoup.inv.repo

import io.peasoup.inv.Home
import io.peasoup.inv.MissingOptionException
import io.peasoup.inv.TempHome
import io.peasoup.inv.run.RunsRoller
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(TempHome.class)
class RepoDescriptorTest {

    @Test
    void ok_with_parameters() {

        def repoParams = RepoHandlerTest.class.getResource("/repo-parameters.json")
        def repoParamsFile = new File(repoParams.path)

        def repoDesc =  new RepoDescriptor(new File("dummy-script.yml"), repoParamsFile)
        repoDesc.name = "repo1"
        repoDesc.loadParametersProperties()

        assertNotNull repoDesc.parametersProperties
        assertNotNull repoDesc.parametersProperties["branch"]
        assertEquals "master", repoDesc.parametersProperties["branch"]
    }

    @Test
    void path_ok() {
        String name = "my.name"
        String path = "my.path"
        File scriptFile = new File(Home.getCurrent(), "repos/repo.groovy")

        def descriptor = new RepoDescriptor(scriptFile)
        descriptor.name(name)

        File expectedRepopath = new File(Home.getReposFolder(), scriptFile.name.split("\\.")[0] + "@" + name)

        assertEquals(RepoDescriptor.DEFAULT_PATH, descriptor.path)
        assertEquals(expectedRepopath, descriptor.repoPath)

        descriptor.path(path)
        assertEquals(new File(expectedRepopath, path), descriptor.repoCompletePath)
    }
    
    @Test
    void properties_file_ok() {
        def parameterFile = new File(Home.getCurrent(), "/test-resources/repo-parameter.json")
        assertTrue parameterFile.exists()

        def descriptor = new RepoDescriptor(new File("dummy-repo.yml"), parameterFile)
        descriptor.name("repo1")

        assertEquals "my-branch", descriptor.propertyMissing("branch")
    }

    @Test
    void properties_file_not_ok() {
        def descriptor = new RepoDescriptor(new File("dummy-repo.yml"))
        assertEquals "\${branch}", descriptor.propertyMissing("branch")
    }

    @Test
    void properties_file_not_ok_2() {
        def parameterFile = new File(Home.getCurrent(), "/test-resources/repo-parameter.json")
        assertTrue parameterFile.exists()

        def descriptor = new RepoDescriptor(new File("dummy-repo.yml"), parameterFile)
        descriptor.name("not-there")

        assertEquals "\${branch}", descriptor.propertyMissing("branch")
    }

    @Test
    void missing_parameter_name() {
        assertThrows(MissingOptionException.class, {
            new RepoDescriptor.AskDescriptor().parameter("", "usage")
        })
    }

    @Test
    void missing_parameter_usage() {
        assertThrows(MissingOptionException.class, {
            new RepoDescriptor.AskDescriptor().parameter("name", "")
        })
    }

    @Test
    void name_invalid_format() {
        assertThrows(IllegalArgumentException.class, {
            new RepoDescriptor.AskDescriptor().parameter("a-b-c", "usage")
        })
    }
}
