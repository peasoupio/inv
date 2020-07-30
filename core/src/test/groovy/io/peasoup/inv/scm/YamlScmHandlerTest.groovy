package io.peasoup.inv.scm

import io.peasoup.inv.TempHome
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TempHome.class)
class YamlScmHandlerTest {

    @Test
    void ok() {
        def executor = new ScmExecutor()

        def yamlFile = new File("../examples/yaml/scm.yaml")
        ScmInvoker.invoke(executor, yamlFile)

        def report = executor.execute()
        assert !report.any { !it.isOk() }
    }

}
