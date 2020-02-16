package io.peasoup.inv.composer

import io.peasoup.inv.TempHome
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.jupiter.api.Assertions.assertThrows

@RunWith(TempHome.class)
class ScmFileCollectionTest {

    String base = "./src/main/example/composer/scms/"
    ScmFileCollection scmFileCollection

    @Before
    void setup() {
        scmFileCollection = new ScmFileCollection(new File(base))
    }

    @Test
    void ok() {
        new ScmFileCollection(new File(base))
    }

    @Test
    void not_ok() {
        assertThrows(AssertionError.class, {
            new ScmFileCollection(null)
        })

        assertThrows(AssertionError.class, {
            new ScmFileCollection(new File("not-existing"))
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
