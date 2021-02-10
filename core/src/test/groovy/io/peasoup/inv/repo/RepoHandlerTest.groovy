package io.peasoup.inv.repo

import io.peasoup.inv.MissingOptionException
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThrows

class RepoHandlerTest {

    File scriptFile
    RepoExecutor executor
    RepoHandler repo

    @Before
    void setup() {
        scriptFile = new File("dummy-repo.yml")
        executor = new RepoExecutor()
        repo = new RepoHandler(executor, scriptFile)
    }

    @Test
    void ok_2() {
        repo {

            name 'test'

            path "my-path"

            src "my-src"

            hooks {

                assertEquals "\${notExisting}", notExisting
                assertEquals "test", name
                assertEquals "my-src", src

            }
        }
    }

    @Test
    void not_ok() {
        assertThrows(IllegalArgumentException.class, {
            new RepoHandler(null, null)
        })

        assertThrows(IllegalArgumentException.class, {
            new RepoHandler(null, new File("dummy-repo.yml"))
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
        assertThrows(MissingOptionException.class, {
            repo.call {
                path "ok"
            }
        })
    }
}





