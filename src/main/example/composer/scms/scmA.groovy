scm {

  name "scm1"

  path ".gitcache/githubHomepage/appA"
  src "https://github.com/peasoupio/inv.git"
  entry 'src/main/example/githubHomepage/appA.groovy'
  timeout 46000

  ask {
    // Exemples
    parameter "simpleParam", "My simple parameter"
    parameter "simpleParamWithDefault", "My simple parameter with a default value", [defaultValue: 'myDefault']
    parameter "staticList", "Using static values", [values: ["my", "values"]]
    parameter "command", "Using a command", [command: "bash -c 'cat src/main/example/githubHomepage/appA.groovy'"]
    parameter "filter", "Using a command with a filter", [
            command: "bash -c 'cat src/main/example/githubHomepage/appA.groovy'",
            filter: {
              if (it.contains("name"))
                return it.split()[1]
            }]
    parameter "filterRegex", "Using a command with a filter", [command: "bash -c 'cat src/main/example/githubHomepage/appA.groovy'", filterRegex: /.*inv\.(\S*).*/]

    parameter "branch", "Select which branch to use", [defaultValue: 'master', command: "git ls-remote ${src}", required: true]
  }

  hooks {

    init """
echo '${param2}'
echo '${param3}'
echo '${param4}'

git clone -b ${branch} ${src} .
    """

    update """
git pull
    """
  }
}

scm {

  name "scm2"

  path ".gitcache/githubHomepage/appB"
  src "https://github.com/peasoupio/inv.git"
  entry 'src/main/example/githubHomepage/appB.groovy'
  timeout 30000

  ask {
    parameter "branch", "Select which branch to use", [defaultValue: 'master', command: "git ls-remote ${src}", filterRegex: /.*refs\/((?:heads|tags).*)/]
  }

  hooks {

    init """
git clone -b ${branch} ${src} .
    """

    update """
git pull
    """
  }
}
