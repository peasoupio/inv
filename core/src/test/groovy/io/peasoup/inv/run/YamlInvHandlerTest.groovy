package io.peasoup.inv.run

import io.peasoup.inv.TempHome
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.assertTrue

@RunWith(TempHome.class)
class YamlInvHandlerTest {

    @Test
    void ok() {
        def yamlFile = new File("../examples/yaml/inv.yaml")

        def executor = new InvExecutor()
        executor.addScript(yamlFile)

        def report = executor.execute()
        assertTrue report.isOk();
    }

}