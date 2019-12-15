"scm5" {

  path "${env.TEMP}/scm/${name}"
  src "https://github.com/.../ServerA.git"
  entry 'serverA.groovy'
  timeout 30000

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

"scm6" {

  path "${env.TEMP}/scm/${name}"
  src "https://github.com/.../ServerB.git"
  entry 'serverB.groovy'
  timeout 30000

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
