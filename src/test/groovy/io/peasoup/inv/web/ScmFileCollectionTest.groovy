package io.peasoup.inv.web

import io.peasoup.inv.scm.ScmDescriptor
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class ScmFileCollectionTest {

    String base = "./src/main/example/web/scms/"
    ScmFileCollection scmFileCollection

    @Before
    void setup() {
        scmFileCollection = new ScmFileCollection(new File(base))
    }

    @Test
    void not_ok() {
        assertThrows(PowerAssertionError.class, {
            new ScmFileCollection(null)
        })

        assertThrows(PowerAssertionError.class, {
            new ScmFileCollection(new File("not-existing"))
        })
    }

    @Test
    void load_not_ok() {
        assertThrows(PowerAssertionError.class, {
            scmFileCollection.load(null)
        })

        assertThrows(PowerAssertionError.class, {
            scmFileCollection.load(new File("not-existing"))
        })
    }


    @Test
    void toMap_filtered() {
        (1..40).each {

            def desc = new ScmDescriptor()
            desc.name = it.toString()

            scmFileCollection.elements.put(desc.name,
                    new ScmFile.SourceFileElement(
                        desc,
                        new File(base, "scmA.groovy")
                    ))
        }

        def output = scmFileCollection.toMap([:], 10, 20)

        assert output.descriptors
        assert output.descriptors.size() == 20
        assert (output.descriptors[19].name as Integer) < 40
    }
}