package io.peasoup.inv.repo

repo {
  name "my-first-repository"
  src "my-src-1"

  hooks {
    init "echo ${name}"
  }
}

repo {
  name "my-second-repository"
  src "my-src-2"

  hooks {
    init "echo ${name}"
  }
}

repo {
  name "my-third-repository"
  src "my-src-3"

  hooks {
    init "echo ${name}"
  }
}