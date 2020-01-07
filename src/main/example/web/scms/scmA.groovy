scm {

  name "scm1"

  path "./src/main/example/githubHomepage"
  src "https://github.com/spring-projects/spring-boot.git"
  entry 'appA.groovy'
  timeout 46000

  ask {
    //parameter "branch", "Select which branch to use", 'master', "git ls-remote ${src}", /.*refs\/((?:heads|tags).*)/
    parameter "command", "Using a command", '', "bash -c 'cat appA.groovy'"
    parameter "commandFilter", "Using a command", '', "bash -c 'cat appA.groovy'", /(.+)/
    parameter "staticList", "My second parameter", '', ["my", "values"]
    parameter "param3", "My third parameter", 'none'
    parameter "param4", "My forth parameter #4"
  }

  hooks {

    init """
echo '${param2}'
echo '${param3}'
echo '${param4}'
    """

    update """
echo "update branch ${branch}"
    """
  }
}

scm {

  name "scm2"

  path "./src/main/example/githubHomepage"
  src "https://github.com/.../AppB.git"
  entry 'appB.groovy'
  timeout 30000

  ask {
    parameter "branch", "Select which branch to use", 'master', "git ls-remote ${src}", /.*refs\/((?:heads|tags).*)/
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
