package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.Logger
import io.peasoup.inv.io.FileUtils

@CompileStatic
class InitCreateCommand implements CliCommand {

    @Override
    int call(Map args = [:]) {
        if (args == null)
            throw new IllegalArgumentException("args")

        String repoName = args["<repoName>"]

        def files = new File(Home.getCurrent(), repoName)
        if (files.exists() && files.listFiles().size() > 0) {
            Logger.warn("Current directory is not empty")
            return 1
        }

        // Create init folder
        files.mkdirs()

        // Create .gitignore file
        def gitIgnore = new File(files, ".gitignore")
        gitIgnore.delete()
        gitIgnore << """# Exclude unnecessary .repos files 
.repos/*
!.repos/*-values.json
"""

        // Create href folder
        def hrefFolder = new File(files, "href")
        hrefFolder.mkdirs()

        def hrefDummy = new File(hrefFolder, "keep-me.txt")
        hrefDummy.delete()
        hrefDummy << "# keep for scm"

        // Create repos folder
        def reposFolder = new File(files, ".repos")
        reposFolder.mkdirs()

        def repoDummy = new File(reposFolder, "keep-me-values.json")
        repoDummy.delete()
        repoDummy << "{ }"

        // Create settings.xml
        def settingsXml = new File(files, "settings.xml")
        settingsXml.delete()
        settingsXml << "{ }"

        // Create init.yml
        def initYml = new File(files, "init.yml")
        initYml.delete()
        initYml << """# Default init file
repo:
  - name: define me ...
    src: define me ...

    hooks:
      init: |
          git clone \${src} .

      pull: |
          git pull
          
      push: |
          git add run.txt
          git add settings.xml
          git add ./*-values.json
          git commit -m "Added Composer changes automatically"
          
      version: |
          git rev-parse --abbrev-ref HEAD
"""

        StringBuilder sb = new StringBuilder()
                .append("You may proceed with any of the following commands:").append(System.lineSeparator())
                .append("\tgit init").append(System.lineSeparator())
                .append("\tgit checkout -b main").append(System.lineSeparator())
                .append("\tgit add --all").append(System.lineSeparator())
                .append("\tgit commit -m \"Initial commit\"").append(System.lineSeparator())
                .append("You also can start composer using the following command:").append(System.lineSeparator())
                .append("\tinv init run init.yml").append(System.lineSeparator())

        Logger.trace(sb.toString())

        Logger.system("GITIGNORE: ${FileUtils.convertUnixPath(gitIgnore.absolutePath)}")
        Logger.system("HREFDUMMY: ${FileUtils.convertUnixPath(hrefDummy.absolutePath)}")
        Logger.system("REPODUMMY: ${FileUtils.convertUnixPath(repoDummy.absolutePath)}")
        Logger.system("SETTINGSXML: ${FileUtils.convertUnixPath(settingsXml.absolutePath)}")
        Logger.system("INITYML: ${FileUtils.convertUnixPath(initYml.absolutePath)}")

        return 0
    }

    @Override
    boolean rolling() {
        return false
    }

    @Override
    String usage() {
        """
Create a new INIT folder named <repoName> at the current INV_HOME location.

Usage:
  inv [-dsx] init-create <repoName>

Arguments:
  <repoName>   The REPO name.
"""
    }

    @Override
    boolean requireSafeExecutionLibraries() {
        return false
    }

}
