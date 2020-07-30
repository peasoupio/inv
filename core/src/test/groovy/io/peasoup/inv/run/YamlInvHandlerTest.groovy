package io.peasoup.inv.run

import io.peasoup.inv.TempHome
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TempHome.class)
class YamlInvHandlerTest {

    @Test
    void ok() {
        def executor = new InvExecutor()

        def yamlFile = new File("../examples/yaml/inv.yaml")
        InvInvoker.invoke(executor, yamlFile)

        def report = executor.execute()
        assert report.isOk();
    }

}