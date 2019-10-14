inv {
    name "my-app-1"

    require inv.Maven using {
        resolved {
            impersonate response.analyze withArgs "app1/pom.xml"
        }
    }
}

