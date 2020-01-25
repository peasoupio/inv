package io.peasoup.inv.scm

import io.peasoup.inv.Main
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

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
        assert !Main.currentHome

        def path = "/my/path"
        def descriptor = new ScmDescriptor()

        descriptor.path(path)
        assert descriptor.path.toString() == new File(path).toString()

        Main.currentHome = new File("./sub/folder").absoluteFile
        assert Main.currentHome.isAbsolute()

        descriptor.path(path)
        assert descriptor.path.absolutePath == new File(Main.currentHome.toString(), path).absolutePath
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
