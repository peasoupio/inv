repo {

  name "my-invalid-repo"

  hooks {

    init """
not-existing
    """
  }
}
