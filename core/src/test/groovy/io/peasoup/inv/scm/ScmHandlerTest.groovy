package io.peasoup.inv.scm


import org.junit.Before
import org.junit.Test

import static junit.framework.Assert.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

class ScmHandlerTest {

    ScmExecutor executor
    ScmHandler scm

    @Before
    void setup() {
        executor = new ScmExecutor()
        scm = new ScmHandler(executor)
    }

    @Test
    void ok() {

        def testScm = ScmHandlerTest.class.getResource("/scm.groovy")
        executor.read(new File(testScm.path))

        assert executor.scms["my-repository"]

        assertEquals executor.scms["my-repository"].src, "https://github.com/spring-guides/gs-spring-boot.git"
        assertEquals executor.scms["my-repository"].entry.size(),  1
        assertEquals executor.scms["my-repository"].entry[0], new File("./src/test/resources/mainTestScript.groovy").absolutePath
        assertEquals executor.scms["my-repository"].timeout, 30000

        assert executor.scms["my-repository"].hooks
        assert executor.scms["my-repository"].hooks.init.contains("mkdir my-repository")
        assert executor.scms["my-repository"].hooks.pull.contains("echo 'update'")
    }

    @Test
    void ok_2() {
        scm {

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
            new ScmHandler(null)
        })
    }

    @Test
    void invalid_scm() {
        assertThrows(IllegalArgumentException.class, {
            scm.call null
        })
    }

    @Test
    void invalid_path() {
        assertThrows(Exception.class, {
            scm.call {
                path 1
            }
        })
    }

    @Test
    void invalid_src() {
        assertThrows(Exception.class, {
            scm.call {
                src 1
            }
        })
    }

    @Test
    void invalid_entry() {
        assertThrows(Exception.class, {
            scm.call {
                entry 1
            }
        })
    }

    @Test
    void invalid_timeout() {
        assertThrows(Exception.class, {
            scm.call {
                timeout "0"
            }
        })
    }

    @Test
    void invalid_hooks() {
        assertThrows(IllegalArgumentException.class, {
            scm.call {
                hooks null
            }
        })
    }

    @Test
    void invalid_ask() {
        assertThrows(IllegalArgumentException.class, {
            scm.call {
                ask null
            }
        })
    }

    @Test
    void missing_name() {
        assertThrows(ScmHandler.SCMOptionRequiredException.class, {
            scm.call {
                path "ok"
            }
        })
    }
}




