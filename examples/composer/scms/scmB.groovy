scm {

  name "scm3"

  path ".gitcache/githubHomepage/iis"
  src "https://github.com/peasoupio/inv.git"
  entry 'examples/githubHomepage/iis.groovy'
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

scm {

  name "scm4"

  path ".gitcache/githubHomepage/kubernetes"
  src "https://github.com/peasoupio/inv.git"
  entry 'examples/githubHomepage/kubernetes.groovy'
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
