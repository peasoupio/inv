repo {

  name "my-invalid-repo"

  path "${env.TEMP ?: '/tmp'}/inv/repo/${name}"

  hooks {

    init """
not-existing
    """
  }
}
