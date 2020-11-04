package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.run.Logger

@CompileStatic
class InitCreateCommand implements CliCommand {

    int call() {

        def files = new File("./").listFiles()
        if (files != null && files.size() > 0) {
            Logger.warn("Current directory is not empty")
            return 1
        }

        // Create .gitignore file
        def gitIgnore = new File(".gitignore")
        gitIgnore.delete()
        gitIgnore << """# Exclude unnecessary .repos files 
.repos/
!.repos/*-values.json
"""

        // Create href folder
        def hrefFolder = new File("href")
        hrefFolder.mkdirs()

        // Create settings.xml
        def settingsXml = new File("settings.xml")
        settingsXml.delete()
        settingsXml << """{
}
"""

        // Create init.yml
        def initYml = new File("init.yml")
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
            .append("\tgit checkout -b main").append(System.lineSeparator())
            .append("\tgit add --all").append(System.lineSeparator())
            .append("\tgit commit -m \"Initial commit\"").append(System.lineSeparator())
            .append("You also can start composer using the following command:").append(System.lineSeparator())
            .append("\tinv init run init.yml").append(System.lineSeparator())

        Logger.trace(sb.toString())

        return 0
    }

    boolean rolling() {
        return false
    }


}
