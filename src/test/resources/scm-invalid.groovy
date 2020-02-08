scm {

  name "my-invalid-scm"

  path "${env.TEMP ?: '/tmp'}/inv/scm/${name}"
  entry "entry1"

  hooks {

    init """
not-existing
    """
  }
}
