inv {
    name "my-app-2"

    require inv.Maven using {
        resolved {
            impersonate response.analyze withArgs "app2/pom.xml"

            println "got my poms"
        }
    }

    ready {
        println "my-app-2 is ready!"
    }
}

