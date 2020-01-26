scm {

  name "my-repository"

  path "${env.TEMP}/inv/scm/${name}"
  src "https://github.com/spring-guides/gs-spring-boot.git"
  entry new File("./src/test/resources/mainTestScript.groovy").absolutePath
  timeout 30000

  ask {
    parameter "remoteBranch", "Select which branch to use", [
      defaultValue: 'master',
      commandValues: "git ls-remote ${src}",
      commandFilter: /.*refs\/((?:heads|tags).*)/
    ]

    parameter "localBranch", "Select which branch to use", [
      defaultValue: 'master',
      staticValues: ["feature", "master"]
    ]
  }

  hooks {

    init """
echo 'init'
pwd
mkdir ${name}
    """

    update """
echo 'update'
    """
  }
}
