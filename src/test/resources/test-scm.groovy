"my-repository" {

  path "${env.TEMP}/scm/${name}"
  src "https://github.com/spring-guides/gs-spring-boot.git"
  entry new File("./src/test/resources/mainTestScript.groovy").absolutePath
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
