import groovy.ant.FileNameFinder

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/*

    Files (I/O) general tool (outside Groovy framework)

    Exposes :
        $files.glob -> Suitable for specific files
        $files.find -> Suitable for performances on generic file patterns

*/

inv {
    broadcast inv.Files using {
        ready {[
            /*
                Locate files based on Ant-style patterns

                @param pwd   parent working directory
                @param glob  the Ant-style pattern to select files
                @param glob  the Ant-style pattern to exclude files
             */
            glob: { String pwd, String glob = "*", String exclude = "" ->
                assert pwd
                assert glob

                return new FileNameFinder().getFileNames(pwd, glob, exclude)
            },

            /*
                Locate files on pure Java framework

                @param pwd   parent working directory
                @param glob  the pattern to select files
                @param glob  the pattern to exclude files
             */
            find: { String pwd, String pattern = "", String exclude = "" ->
                assert pwd

                Files.walk(Paths.get(pwd))
                        .parallel()
                        .filter({ Path p -> Files.isRegularFile(p) })
                        .collect{ Path p -> p.toFile() }
                        .findAll { File f -> !exclude || !f.absolutePath.contains(exclude) }
                        .findAll { File f -> !pattern || f.absolutePath.contains(pattern) }
            }

        ]}
    }
}