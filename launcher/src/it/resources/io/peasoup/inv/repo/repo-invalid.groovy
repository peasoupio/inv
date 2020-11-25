package io.peasoup.inv.repo

repo {

  name "my-invalid-repo"

  hooks {

    init """
exit 1
    """
  }
}
