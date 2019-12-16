"scm3" {

  path "D:/workspace/inv/src/main/example/githubHomepage"
  src "https://github.com/.../iis.git"
  entry 'iis.groovy'
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

"scm4" {

  path "D:/workspace/inv/src/main/example/githubHomepage"
  src "https://github.com/.../kubernetes.git"
  entry 'kubernetes.groovy'
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
