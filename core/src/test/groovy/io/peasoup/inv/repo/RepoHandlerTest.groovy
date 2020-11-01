package io.peasoup.inv.repo


import org.junit.Before
import org.junit.Test

import static junit.framework.Assert.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

class RepoHandlerTest {

    RepoExecutor executor
    RepoHandler repo

    @Before
    void setup() {
        executor = new RepoExecutor()
        repo = new RepoHandler(executor)
    }

    @Test
    void ok() {

        def testRepo = RepoHandlerTest.class.getResource("/repo.groovy")
        executor.parse(new File(testRepo.path))

        assert executor.repos["my-repository"]

        assertEquals "https://github.com/spring-guides/gs-spring-boot.git", executor.repos["my-repository"].src
        assertEquals 30000, executor.repos["my-repository"].timeout

        assert executor.repos["my-repository"].hooks
        assert executor.repos["my-repository"].hooks.init.contains("mkdir something")
        assert executor.repos["my-repository"].hooks.pull.contains("echo 'pull'")
    }

    @Test
    void ok_2() {
        repo {

            name 'test'

            path "my-path"

            src "my-src"

            hooks {

                assert notExisting == "\${notExisting}"
                assert name == "test"
                assert src == "my-src"

            }
        }
    }

    @Test
    void not_ok() {
        assertThrows(IllegalArgumentException.class, {
            new RepoHandler(null)
        })
    }

    @Test
    void invalid_repo() {
        assertThrows(IllegalArgumentException.class, {
            repo.call null
        })
    }

    @Test
    void invalid_path() {
        assertThrows(Exception.class, {
            repo.call {
                path 1
            }
        })
    }

    @Test
    void invalid_src() {
        assertThrows(Exception.class, {
            repo.call {
                src 1
            }
        })
    }

    @Test
    void invalid_timeout() {
        assertThrows(Exception.class, {
            repo.call {
                timeout "0"
            }
        })
    }

    @Test
    void invalid_hooks() {
        assertThrows(IllegalArgumentException.class, {
            repo.call {
                hooks null
            }
        })
    }

    @Test
    void invalid_ask() {
        assertThrows(IllegalArgumentException.class, {
            repo.call {
                ask null
            }
        })
    }

    @Test
    void missing_name() {
        assertThrows(RepoHandler.RepoOptionRequiredException.class, {
            repo.call {
                path "ok"
            }
        })
    }
}





