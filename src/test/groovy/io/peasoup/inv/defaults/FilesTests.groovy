package io.peasoup.inv.defaults

import io.peasoup.inv.InvHandler
import io.peasoup.inv.InvInvoker
import io.peasoup.inv.Logger
import io.peasoup.inv.utils.Stdout
import org.junit.Before
import org.junit.Test

class FilesTests {

    @Before
    void setup() {
        ExpandoMetaClass.enableGlobally()
        Logger.DebugModeEnabled = true
    }

    @Test
    void glob() {

        def files = FilesTests.class.getResource("/defaults/files").path

        def inv = new InvHandler()

        InvInvoker.invoke(inv, new File("./defaults/files/inv.groovy"))

        inv {
            require inv.Files into '$files'

            step {
                $files.glob(files).each { println it + "-GLOB-ALL" }
                $files.glob(files, "*file*").each { println it + "-GLOB-PATTERN" }
                $files.glob(files, "*file*", "*file2*").each { println it + "-GLOB-EXCLUDE" }
            }
        }


        Stdout.capture ({ inv() }, {
            // GLOB All
            assert it.contains("file1-GLOB-ALL")
            assert it.contains("file2-GLOB-ALL")

            // GLOB Pattern
            assert it.contains("file1-GLOB-ALL")
            assert it.contains("file2-GLOB-ALL")

            // GLOB Exclude
            assert it.contains("file1-GLOB-EXCLUDE")
            assert !it.contains("file2-GLOB-EXCLUDE")
        })
    }

    @Test
    void find() {

        def files = new File(FilesTests.class.getResource("/defaults/files").path).absolutePath
        def inv = new InvHandler()

        InvInvoker.invoke(inv, new File("./defaults/files/inv.groovy"))

        inv {
            require inv.Files into '$files'

            step {
                $files.find(files).each { println it.path + "-FIND-ALL" }
                $files.find(files, "file").each { println it.path + "-FIND-PATTERN" }
                $files.find(files, "file", "file2").each { println it.path + "-FIND-EXCLUDE" }
            }
        }


        Stdout.capture ({ inv() }, {
            // Find All
            assert it.contains("file1-FIND-ALL")
            assert it.contains("file2-FIND-ALL")

            // Find Pattern
            assert it.contains("file1-FIND-PATTERN")
            assert it.contains("file2-FIND-PATTERN")

            // Find Exclude
            assert it.contains("file1-FIND-EXCLUDE")
            assert !it.contains("file2-FIND-EXCLUDE")
        })
    }
}