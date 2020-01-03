scm {

  name "scm5"

  path "D:/workspace/inv/src/main/example/githubHomepage"
  src "https://github.com/.../ServerA.git"
  entry 'serverA.groovy'
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

scm {

  name "scm6"

  path "D:/workspace/inv/src/main/example/githubHomepage"
  src "https://github.com/.../ServerB.git"
  entry 'serverB.groovy'
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
