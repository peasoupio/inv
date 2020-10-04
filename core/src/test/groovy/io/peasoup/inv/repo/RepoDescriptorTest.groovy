package io.peasoup.inv.repo

import io.peasoup.inv.Home
import io.peasoup.inv.TempHome
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.jupiter.api.Assertions.assertThrows

@RunWith(TempHome.class)
class RepoDescriptorTest {

    @Test
    void ok_with_parameters() {

        def repoParams = RepoHandlerTest.class.getResource("/repo-parameters.json")
        def repoParamsFile = new File(repoParams.path)

        def repoDesc =  new RepoDescriptor(repoParamsFile)
        repoDesc.name = "repo1"
        repoDesc.loadParametersProperties()

        assert repoDesc.parametersProperties
        assert repoDesc.parametersProperties["branch"]
        assert repoDesc.parametersProperties["branch"] == "master"
    }

    @Test
    void path_using_invHome() {
        def relativePath = "./my/path"
        def absolutePath = new File(Home.getCurrent(), "/my/path").absolutePath
        def descriptor = new RepoDescriptor()

        descriptor.path(relativePath)
        assert descriptor.path.absolutePath == new File(Home.getCurrent(), relativePath).absolutePath

        descriptor.path(absolutePath)
        assert descriptor.path.toString() == absolutePath
    }
    
    @Test
    void properties_file_ok() {
        def parameterFile = new File(Home.getCurrent(), "/test-resources/repo-parameter.json")
        assert parameterFile.exists()

        def descriptor = new RepoDescriptor(parameterFile)
        descriptor.name("repo1")

        assert descriptor.propertyMissing("branch") == "my-branch"
    }

    @Test
    void properties_file_not_ok() {
        def descriptor = new RepoDescriptor()
        assert descriptor.propertyMissing("branch") == "\${branch}"
    }

    @Test
    void properties_file_not_ok_2() {
        def parameterFile = new File(Home.getCurrent(), "/test-resources/repo-parameter.json")
        assert parameterFile.exists()

        def descriptor = new RepoDescriptor(parameterFile)
        descriptor.name("not-there")

        assert descriptor.propertyMissing("branch") == "\${branch}"
    }

    @Test
    void missing_parameter_name() {
        assertThrows(RepoHandler.RepoOptionRequiredException.class, {
            new RepoDescriptor.AskDescriptor().parameter("", "usage")
        })
    }

    @Test
    void missing_parameter_usage() {
        assertThrows(RepoHandler.RepoOptionRequiredException.class, {
            new RepoDescriptor.AskDescriptor().parameter("name", "")
        })
    }
}
