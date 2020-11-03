package io.peasoup.inv.repo

import io.peasoup.inv.TempHome
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.jupiter.api.Assertions.assertFalse

@RunWith(TempHome.class)
class YamlRepoHandlerTest {

    @Test
    void ok() {
        RepoInvoker.newCache()

        def executor = new RepoExecutor()

        def yamlFile = new File("../examples/yaml/repo.yaml")
        RepoInvoker.invoke(executor, yamlFile)

        def report = executor.execute()
        assertFalse report.any { !it.isOk() }
    }

}
