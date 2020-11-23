package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Logger
import io.peasoup.inv.graph.DeltaGraph
import spark.utils.StringUtils

@CompileStatic
class DeltaCommand implements CliCommand {

    @Override
    int call(Map args = [:]) {
        if (args == null)
            throw new IllegalArgumentException("args")

        String base = args["<base>"]
        String other = args["<other>"]

        if (StringUtils.isEmpty(base))
            return 1

        if (StringUtils.isEmpty(other))
            return 2

        def delta = new DeltaGraph(
                new File(base).newReader(),
                new File(other).newReader())
        delta.resolve()

        Logger.trace(delta.echo())

        return 0
    }

    @Override
    boolean rolling() {
        return false
    }

    @Override
    String usage() {
        """
Generate delta between two run files.

Usage:
  inv [-dsx] delta <base> <other>

Arguments:
  <base>       Base file location
  <other>      Other file location
"""
    }
}
