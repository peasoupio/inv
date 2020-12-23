package io.peasoup.inv.run

import io.peasoup.inv.TempHome
import io.peasoup.inv.loader.YamlLoader
import io.peasoup.inv.run.yaml.YamlInvHandler
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.assertThrows
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

    @Test
    void not_ok() {
        assertThrows(IllegalArgumentException.class, {
            new YamlInvHandler(null, null, null, null, null, null)
        })

        assertThrows(IllegalArgumentException.class, {
            new YamlInvHandler(new InvExecutor(), null, null, null, null, null)
        })

        assertThrows(IllegalArgumentException.class, {
            new YamlInvHandler(new InvExecutor(), new YamlLoader(), null, null, null, null)
        })

        assertThrows(IllegalArgumentException.class, {
            new YamlInvHandler(new InvExecutor(), new YamlLoader(), new File("file.yml"), null, null, null)
        })

        assertThrows(IllegalArgumentException.class, {
            new YamlInvHandler(new InvExecutor(), new YamlLoader(), new File("file.yml"), "pwd", null, null)
        })

        assertThrows(IllegalArgumentException.class, {
            new YamlInvHandler(new InvExecutor(), new YamlLoader(), new File("file.yml"), "pwd", "repo", null)
        })
    }

}