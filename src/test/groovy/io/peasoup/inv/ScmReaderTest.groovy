package io.peasoup.inv

import org.junit.Test

class ScmReaderTest {

    @Test
    void ctor() {
        def files = new ScmReader(new File("./src/test/resources/.scm")).execute()

        println files
    }
}