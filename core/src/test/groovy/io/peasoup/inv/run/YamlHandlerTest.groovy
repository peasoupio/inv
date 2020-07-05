package io.peasoup.inv.run


import io.peasoup.inv.run.yaml.YamlHandler
import org.junit.Test

class YamlHandlerTest {

    @Test
    void ok() {

        def yamlResource = YamlHandler.class.getResource("/inv-invoker-yamlfile.yaml")
        assert yamlResource

        def executor = new InvExecutor()

        def yamlFile = new File(yamlResource.path)
        InvInvoker.invoke(executor, yamlFile)

        def report = executor.execute()
        assert report.isOk();
    }

}