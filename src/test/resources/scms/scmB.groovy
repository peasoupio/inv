"scm3" {

  path "${env.TEMP}/scm/${name}"
  src "https://github.com/.../iis.git"
  entry 'iis.groovy'
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

"scm4" {

  path "${env.TEMP}/scm/${name}"
  src "https://github.com/.../kubernetes.git"
  entry 'kubernetes.groovy'
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
