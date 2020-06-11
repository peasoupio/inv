package io.peasoup.inv.scm

import io.peasoup.inv.Home
import io.peasoup.inv.TempHome
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.jupiter.api.Assertions.assertThrows

@RunWith(TempHome.class)
class ScmDescriptorTest {

    @Test
    void ok_with_parameters() {

        def scmParams = ScmHandlerTest.class.getResource("/scm-parameters.json")
        def scmParamsFile = new File(scmParams.path)

        def scmDesc =  new ScmDescriptor(scmParamsFile)
        scmDesc.name = "scm1"
        scmDesc.loadParametersProperties()

        assert scmDesc.parametersProperties
        assert scmDesc.parametersProperties["branch"]
        assert scmDesc.parametersProperties["branch"] == "master"
    }

    @Test
    void path_using_invHome() {
        def relativePath = "./my/path"
        def absolutePath = new File(Home.getCurrent(), "/my/path").absolutePath
        def descriptor = new ScmDescriptor()

        descriptor.path(relativePath)
        assert descriptor.path.absolutePath == new File(Home.getCurrent(), relativePath).absolutePath

        descriptor.path(absolutePath)
        assert descriptor.path.toString() == absolutePath
    }
    
    @Test
    void properties_file_ok() {
        def parameterFile = new File(Home.getCurrent(), "/test-resources/scm-parameter.json")
        assert parameterFile.exists()

        def descriptor = new ScmDescriptor(parameterFile)
        descriptor.name("scm1")

        assert descriptor.propertyMissing("branch") == "my-branch"
    }

    @Test
    void properties_file_not_ok() {
        def descriptor = new ScmDescriptor()
        assert descriptor.propertyMissing("branch") == "\${branch}"
    }

    @Test
    void properties_file_not_ok_2() {
        def parameterFile = new File(Home.getCurrent(), "/test-resources/scm-parameter.json")
        assert parameterFile.exists()

        def descriptor = new ScmDescriptor(parameterFile)
        descriptor.name("not-there")

        assert descriptor.propertyMissing("branch") == "\${branch}"
    }

    @Test
    void missing_parameter_name() {
        assertThrows(ScmHandler.SCMOptionRequiredException.class, {
            new ScmDescriptor.AskDescriptor().parameter("", "usage")
        })
    }

    @Test
    void missing_parameter_usage() {
        assertThrows(ScmHandler.SCMOptionRequiredException.class, {
            new ScmDescriptor.AskDescriptor().parameter("name", "")
        })
    }
}
