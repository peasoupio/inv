package io.peasoup.inv.scm

import org.junit.Test

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
}
