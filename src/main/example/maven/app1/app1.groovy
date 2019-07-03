inv {
    name "my-app-1"

    require inv.Maven using {
        resolved {
            impersonate response.analyze with "app1/pom.xml"
        }
    }
}

