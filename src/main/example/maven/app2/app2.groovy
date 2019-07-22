inv {
    name "my-app-2"

    require inv.Maven using {
        resolved {
            impersonate response.analyze with "app2/pom.xml"
        }
    }

    ready {
        println "my-app-2 is ready!"
    }
}

