"scm1" {
  
  path "${env.TEMP}/scm/${name}"
  src "https://github.com/spring-projects/spring-boot.git"
  entry 'appA.groovy'
  timeout 46000
  
  ask {
    // variable, description, default value, how to get values, filter values
    parameter "branch", "Select which branch to use", 'master', "git ls-remote ${src}", /.*refs\/((?:heads|tags).*)/
    parameter "param2", "My second parameter", '', ["my", "values"]
    parameter "param3", "My third parameter"
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

"scm2" {

  path "${env.TEMP}/scm/${name}"
  src "https://github.com/.../AppB.git"
  entry 'appB.groovy'
  timeout 30000

  hooks {

    init """
echo 'init'
    """

    update """
echo 'update'
    """
  }
}
