scm {

  name "scm5"

  path ".gitcache/githubHomepage/serverA"
  src "https://github.com/peasoupio/inv.git"
  entry 'src/main/example/githubHomepage/serverA.groovy'
  timeout 30000

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

  name "scm6"

  path ".gitcache/githubHomepage/serverB"
  src "https://github.com/peasoupio/inv.git"
  entry 'src/main/example/githubHomepage/serverB.groovy'
  timeout 30000

  hooks {

    init """
git clone -b ${branch} ${src} .
    """

    update """
git pull
    """
  }
}
