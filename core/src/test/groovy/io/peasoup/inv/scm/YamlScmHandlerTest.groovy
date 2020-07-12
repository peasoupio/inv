package io.peasoup.inv.scm

import io.peasoup.inv.TempHome;
import io.peasoup.inv.scm.yaml.YamlScmHandler;
import org.junit.Test
import org.junit.runner.RunWith;

@RunWith(TempHome.class)
class YamlScmHandlerTest {

    @Test
    void ok() {

        def yamlResource = YamlScmHandler.class.getResource("/scm-invoker-yamlfile.yaml")
        assert yamlResource

        def executor = new ScmExecutor()

        def yamlFile = new File(yamlResource.path)
        ScmInvoker.invoke(executor, yamlFile)

        def report = executor.execute()
        assert !report.any { !it.isOk() }
    }

}
