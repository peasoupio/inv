package defaults.files

inv {
    require inv.Files into '$files'

    step {
        $files.glob(pwd).each { println it + "-GLOB-ALL" }
        $files.glob(pwd, "*file*").each { println it + "-GLOB-PATTERN" }
        $files.glob(pwd, "*file*", "*file2*").each { println it + "-GLOB-EXCLUDE" }

        $files.find(pwd).each { println it.path + "-FIND-ALL" }
        $files.find(pwd, "file").each { println it.path + "-FIND-PATTERN" }
        $files.find(pwd, "file", "groovy").each { println it.path + "-FIND-EXCLUDE" }
    }
}

