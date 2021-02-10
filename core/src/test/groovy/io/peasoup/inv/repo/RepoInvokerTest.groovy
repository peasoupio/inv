package io.peasoup.inv.repo


import io.peasoup.inv.TempHome
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.assertThrows
import static org.junit.Assert.assertTrue

@RunWith(TempHome.class)
class RepoInvokerTest {

    RepoExecutor repoExecutor
    RepoInvoker repoInvoker

    @Before
    void setup() {
        repoExecutor = new RepoExecutor()
        repoInvoker = repoExecutor.repoInvoker
    }

    @Test
    void not_existing_script() {
        def repoFile =  new File('/repo-does-not-exists.groovy')
        repoInvoker.invokeScript(repoFile, null)

        assertTrue repoExecutor.repos.isEmpty()
    }

    @Test
    void invoke_not_ok() {
        // inv invoker is null
        assertThrows(IllegalArgumentException.class, {
            repoInvoker.invokeScript(null, null)
        })

        // script file is null
        assertThrows(IllegalArgumentException.class, {
            repoInvoker.invokeScript(null)
        })

        // parameters file is null
        assertThrows(IllegalArgumentException.class, {
            RepoInvoker.expectedParametersfileLocation((File)null)
        })

        // parameters file is null
        assertThrows(IllegalArgumentException.class, {
            RepoInvoker.expectedParametersfileLocation((String)null)
        })

    }
}