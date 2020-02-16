package io.peasoup.inv.scm

import io.peasoup.inv.Main
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
        def relativePath = "/my/path"
        def absolutePath = new File(Main.currentHome, "/my/path").absolutePath
        def descriptor = new ScmDescriptor()

        descriptor.path(relativePath)
        assert descriptor.path.absolutePath == new File(Main.currentHome, relativePath).absolutePath

        descriptor.path(absolutePath)
        assert descriptor.path.toString() == absolutePath
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
