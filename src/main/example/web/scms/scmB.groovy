scm {

  name "scm3"

  path "./src/main/example/githubHomepage"
  src "https://github.com/.../iis.git"
  entry 'iis.groovy'
  timeout 30000

  ask {
    parameter "branch", "Select which branch to use", [defaultValue: 'master', command: "git ls-remote ${src}", filterRegex: /.*refs\/((?:heads|tags).*)/]
  }

  hooks {

    init """
echo 'init'
    """

    update """
echo 'update'
    """
  }
}

scm {

  name "scm4"

  path "./src/main/example/githubHomepage"
  src "https://github.com/.../kubernetes.git"
  entry 'kubernetes.groovy'
  timeout 30000

  ask {
    parameter "branch", "Select which branch to use", [defaultValue: 'master', command: "git ls-remote ${src}", filterRegex: /.*refs\/((?:heads|tags).*)/]
  }

  hooks {

    init """
echo 'init'
    """

    update """
echo 'update'
    """
  }
}
