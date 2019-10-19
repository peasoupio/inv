{
  "my-repository": {
    "path": "${env.TEMP}/scm/${name}",
    "src": "https://github.com/spring-guides/gs-spring-boot.git",
    "entry": "inv.groovy",
    "hooks": {
      "init": [
        "git clone ${src} ${path}"
      ],
      "update": [
        "cd ${path}",
        "git pull"
      ]
    }
  }
}