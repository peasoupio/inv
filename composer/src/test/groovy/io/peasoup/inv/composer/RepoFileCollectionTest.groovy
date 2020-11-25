package io.peasoup.inv.composer

import io.peasoup.inv.TempHome
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(TempHome.class)
class RepoFileCollectionTest {

    String repos = "../examples/composer/.repos/"
    String hrefs = "../examples/composer/.repos/"
    RepoFileCollection repoFileCollection

    @Before
    void setup() {
        def repoFolder = new File(repos)
        def hrefsFolder = new File(hrefs)
        repoFileCollection = new RepoFileCollection(repoFolder, hrefsFolder)

        // Load repo files
        repoFolder.listFiles().each {
            if (!it.name.endsWith(".groovy"))
                return

            repoFileCollection.load(it)
        }
    }

    @Test
    void ok() {
        new RepoFileCollection(new File(repos), new File(hrefs))
    }

    @Test
    void not_ok() {
        assertThrows(IllegalArgumentException.class, {
            new RepoFileCollection(null, null)
        })

        assertThrows(IllegalArgumentException.class, {
            new RepoFileCollection(new File("not-existing"), null)
        })
    }

    @Test
    void load_ok() {
        def file = new File(repos, "repoA.groovy")
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
