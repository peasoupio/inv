scm {

  name "my-invalid-scm"

  path "${env.TEMP}/scm/${name}"
  entry "entry1"

  hooks {

    init """
not-existing
    """
  }
}
