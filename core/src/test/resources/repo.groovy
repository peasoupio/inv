repo {

  name "my-repository"

  src "https://github.com/spring-guides/gs-spring-boot.git"
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
mkdir something
    """

    pull """
echo 'pull'
    """
  }
}
