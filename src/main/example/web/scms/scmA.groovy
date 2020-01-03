scm {

  name "scm1"

  path "D:/workspace/inv/src/main/example/githubHomepage"
  src "https://github.com/spring-projects/spring-boot.git"
  entry 'appA.groovy'
  timeout 46000

  ask {
    //parameter "branch", "Select which branch to use", 'master', "git ls-remote ${src}", /.*refs\/((?:heads|tags).*)/
    parameter "branch", "Select which branch to use", 'master', "bash -c 'echo 1 && echo 2'", /(.+)/
    parameter "param2", "My second parameter", '', ["my", "values"]
    parameter "param3", "My third parameter", 'none', ['my-value']
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

  path "D:/workspace/inv/src/main/example/githubHomepage"
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
