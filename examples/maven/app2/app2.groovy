inv {
    name "my-app-2"

    require { Maven } using {
        resolved {
            analyze("app2/pom.xml")

            println "got my poms"
        }
    }

    ready {
        println "my-app-2 is ready!"
    }
}

