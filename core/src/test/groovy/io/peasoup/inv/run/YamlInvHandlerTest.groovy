package io.peasoup.inv.run

import io.peasoup.inv.TempHome
import io.peasoup.inv.run.yaml.YamlInvHandler
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TempHome.class)
class YamlInvHandlerTest {

    @Test
    void ok() {

        def yamlResource = YamlInvHandler.class.getResource("/inv-invoker-yamlfile.yaml")
        assert yamlResource

        def executor = new InvExecutor()

        def yamlFile = new File(yamlResource.path)
        InvInvoker.invoke(executor, yamlFile)

        def report = executor.execute()
        assert report.isOk();
    }

}