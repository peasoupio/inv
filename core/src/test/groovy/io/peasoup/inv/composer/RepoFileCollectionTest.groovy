package io.peasoup.inv.composer

import io.peasoup.inv.TempHome
import io.peasoup.inv.repo.RepoInvoker
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.jupiter.api.Assertions.*

@RunWith(TempHome.class)
class RepoFileCollectionTest {

    String base = "../examples/composer/repos/"
    RepoFileCollection repoFileCollection

    @Before
    void setup() {
        RepoInvoker.newCache()

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
        assertTrue file.exists()

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
        assertNotNull repoFileCollection.toFiles()
        assertTrue repoFileCollection.toFiles(["undefined"]).isEmpty()
        assertFalse repoFileCollection.toFiles(["repo1"]).isEmpty()
    }
}
