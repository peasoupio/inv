package io.peasoup.inv.repo

import io.peasoup.inv.TempHome
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TempHome.class)
class YamlRepoHandlerTest {

    @Test
    void ok() {
        def executor = new RepoExecutor()

        def yamlFile = new File("../examples/yaml/repo.yaml")
        RepoInvoker.invoke(executor, yamlFile)

        def report = executor.execute()
        assert !report.any { !it.isOk() }
    }

}
