package io.peasoup.inv

import org.junit.Test

class ScmReaderTest {

    @Test
    void ctor() {

        def scmFile =  new File(getClass().getResource('/.scm').toURI())
        def files = new ScmReader(scmFile.newReader()).execute()

        assert files["my-repository"]
    }
}