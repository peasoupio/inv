"my-repository" {

  path "${env.TEMP}/scm/${name}"
  src "https://github.com/spring-guides/gs-spring-boot.git"
  entry "inv.groovy"
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
