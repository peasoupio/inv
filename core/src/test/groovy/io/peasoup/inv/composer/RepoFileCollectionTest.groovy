package io.peasoup.inv.composer

import io.peasoup.inv.TempHome
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.jupiter.api.Assertions.assertThrows

@RunWith(TempHome.class)
class RepoFileCollectionTest {

    String base = "../examples/composer/repos/"
    RepoFileCollection repoFileCollection

    @Before
    void setup() {
        def repoFolder = new File(base)
        repoFileCollection = new RepoFileCollection(repoFolder)

        // Load repo files
        repoFolder.listFiles().each {
            repoFileCollection.load(it)
        }
    }

    @Test
    void ok() {
        new RepoFileCollection(new File(base))
    }

    @Test
    void not_ok() {
        assertThrows(AssertionError.class, {
            new RepoFileCollection(null)
        })

        assertThrows(AssertionError.class, {
            new RepoFileCollection(new File("not-existing"))
        })
    }

    @Test
    void load_ok() {
        def file = new File(base, "repoA.groovy")
        assert file.exists()

        repoFileCollection.load(file)
    }

    @Test
    void load_not_ok() {
        assertThrows(AssertionError.class, {
            repoFileCollection.load(null)
        })

        assertThrows(AssertionError.class, {
            repoFileCollection.load(new File("not-existing"))
        })
    }

    @Test
    void toFiles() {

        assert repoFileCollection.toFiles()
        assert repoFileCollection.toFiles(["undefined"]).isEmpty()
        assert repoFileCollection.toFiles(["repo1"]).size() == 1
    }
}
