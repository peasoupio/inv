package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.fs.Pattern
import io.peasoup.inv.run.InvExecutor

@CompileStatic
class RunCommand implements CliCommand {

    @Override
    int call(Map args = [:]) {
        if (args == null)
            throw new IllegalArgumentException("args")

        List<String> includes = args["<include>"] as List<String>
        String exclude = args["--exclude"]

        if (includes == null)
            return 1

        if (includes.isEmpty())
            return 2

        // Handle excluding patterns
        def excludePatterns = [".runs/*"]
        if (exclude)
            excludePatterns.add(exclude)

        def invExecutor = new InvExecutor()
        List<File> invFiles = Pattern.get(includes, excludePatterns, Home.getCurrent())

        invFiles.sort(new Comparator<File>() {
            @Override
            int compare(File o1, File o2) {
                return o1.getAbsolutePath() <=> o2.getAbsolutePath()
            }
        })

        // Parse INV Groovy files
        invFiles.each {
            invExecutor.addScript(it)
        }

        // Do the actual execution
        if (!invExecutor.execute().isOk())
            return 3

        return 0
    }

    @Override
    boolean rolling() {
        return true
    }

    @Override
    String usage() {
        """
Load and execute INV files.

Usage:
  inv [-dsx] run [--exclude <exclude>] <include>...

Options:
  -e, --exclude=exclude
               Indicates the files to exclude.
               Exclusion is predominant over inclusion
               It is Ant-compatible 
               (p.e *.groovy, ./**/*.groovy, ...)

Arguments:
  <include>    Indicates the files to include.
               It is Ant-compatible 
               (p.e *.groovy, ./**/*.groovy, ...)
               It is also expandable using a space-separator
               (p.e myfile1.groovy myfile2.groovy)
"""
    }

    @Override
    boolean requireSafeExecutionLibraries() {
        return true
    }
}
