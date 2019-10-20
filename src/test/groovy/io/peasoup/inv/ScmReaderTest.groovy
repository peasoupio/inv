package io.peasoup.inv

import org.junit.Test

class ScmReaderTest {

    @Test
    void ctor() {

        String scm = '''
{
  "my-repository": {
    "path": "${env.TEMP}/scm/${name}",
    "src": "https://github.com/spring-guides/gs-spring-boot.git",
    "entry": "inv.groovy",
    "hooks": {
      "init": [
        "git clone ${src} ${path}"
      ],
      "update": [
        "cd ${path}",
        "git pull"
      ]
    }
  }
}
'''
        
        def files = new ScmReader(new StringReader(scm)).execute()

        assert files["my-repository"]
    }
}