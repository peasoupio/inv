repo {

  name "my-invalid-repo"

  path "${env.TEMP ?: '/tmp'}/inv/repo/${name}"
  entry "entry1"

  hooks {

    init """
not-existing
    """
  }
}
