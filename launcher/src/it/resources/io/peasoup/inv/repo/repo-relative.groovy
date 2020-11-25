package io.peasoup.inv.repo

repo {

  name "my-repository-relative"

  path "test-resources"
  src "https://github.com/spring-guides/gs-spring-boot.git"

  hooks {
    init 'echo \'init\''
    pull 'echo \'pull\''
  }
}
