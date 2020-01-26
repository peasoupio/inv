scm {

  name "my-invalid-scm"

  path "${env.TEMP}/inv/scm/${name}"
  entry "entry1"

  hooks {

    init """
not-existing
    """
  }
}
