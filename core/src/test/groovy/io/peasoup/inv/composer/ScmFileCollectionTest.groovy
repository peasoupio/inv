package io.peasoup.inv.composer

import io.peasoup.inv.TempHome
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.jupiter.api.Assertions.assertThrows

@RunWith(TempHome.class)
class ScmFileCollectionTest {

    String base = "../examples/composer/scms/"
    File params = new File("../examples/composer/parameters/")
    ScmFileCollection scmFileCollection

    @Before
    void setup() {
        def scmFolder = new File(base)
        scmFileCollection = new ScmFileCollection(scmFolder, params)

        // Load scm files
        scmFolder.listFiles().each {
            scmFileCollection.load(it)
        }
    }

    @Test
    void ok() {
        new ScmFileCollection(new File(base), params)
    }

    @Test
    void not_ok() {
        assertThrows(AssertionError.class, {
            new ScmFileCollection(null, params)
        })

        assertThrows(AssertionError.class, {
            new ScmFileCollection(new File("not-existing"), params)
        })

        assertThrows(AssertionError.class, {
            new ScmFileCollection(new File(base), null)
        })
    }

    @Test
    void load_ok() {
        def file = new File(base, "scmA.groovy")
        assert file.exists()

        scmFileCollection.load(file)
    }

    @Test
    void load_not_ok() {
        assertThrows(AssertionError.class, {
            scmFileCollection.load(null)
        })

        assertThrows(AssertionError.class, {
            scmFileCollection.load(new File("not-existing"))
        })
    }

    @Test
    void toFiles() {

        assert scmFileCollection.toFiles()
        assert scmFileCollection.toFiles(["undefined"]).isEmpty()
        assert scmFileCollection.toFiles(["scm1"]).size() == 1
    }
}
