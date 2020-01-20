scm {

  name "scm1"

  path "./src/main/example/githubHomepage"
  src "https://github.com/spring-projects/spring-boot.git"
  entry 'appA.groovy'
  timeout 46000

  ask {
    parameter "simpleParam", "My simple parameter"
    parameter "simpleParamWithDefault", "My simple parameter with a default value", [defaultValue: 'myDefault']
    parameter "staticList", "Using static values", [values: ["my", "values"]]
    parameter "command", "Using a command", [command: "bash -c 'cat appA.groovy'"]
    parameter "filter", "Using a command with a filter", [
            command: "bash -c 'cat appA.groovy'",
            filter: {
              if (it.contains("name"))
                return it.split()[1]
            }]
    parameter "filterRegex", "Using a command with a filter", [command: "bash -c 'cat appA.groovy'", filterRegex: /.*inv\.(\S*).*/]
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
