scm {

  name "my-repository-relative"

  path "test-resources"
  src "https://github.com/spring-guides/gs-spring-boot.git"
  entry 'mainTestScript.groovy'

  hooks {
    init 'echo \'init\''
    update 'echo \'update\''
  }
}
