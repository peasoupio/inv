repo {

  name "repo5"

  path ".gitcache/githubHomepage/serverA"
  src "https://github.com/peasoupio/inv.git"
  entry 'examples/githubHomepage/serverA.groovy'
  timeout 30000

  hooks {

    init """
git clone ${src} .
    """

    pull """
git pull
    """
  }
}

repo {

  name "repo6"

  path ".gitcache/githubHomepage/serverB"
  src "https://github.com/peasoupio/inv.git"
  entry 'examples/githubHomepage/serverB.groovy'
  timeout 30000

  hooks {

    init """
git clone ${src} .
    """

    pull """
git pull
    """
  }
}
