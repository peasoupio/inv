repo {

  name "repo3"

  path ".gitcache/githubHomepage/iis"
  src "https://github.com/peasoupio/inv.git"
  timeout 30000

  ask {
    parameter "branch", "Select which branch to use", [defaultValue: 'master', command: "git ls-remote ${src}", filterRegex: /.*refs\/((?:heads|tags).*)/]
  }

  hooks {

    init """
git clone -b ${branch} ${src} .
    """

    pull """
git pull
    """
  }
}

repo {

  name "repo4"

  path ".gitcache/githubHomepage/kubernetes"
  src "https://github.com/peasoupio/inv.git"
  timeout 30000

  ask {
    parameter "branch", "Select which branch to use", [defaultValue: 'master', command: "git ls-remote ${src}", filterRegex: /.*refs\/((?:heads|tags).*)/]
  }

  hooks {

    init """
git clone -b ${branch} ${src} .
    """

    pull """
git pull
    """
  }
}
