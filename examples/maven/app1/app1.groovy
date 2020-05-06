inv {
    name "my-app-1"

    require $inv.Maven using {
        resolved {
            analyze("app1/pom.xml")
        }
    }
}

