inv {
    name "my-app-1"

    require { Maven } using {
        resolved {
            analyze("app1/pom.xml")
        }
    }
}

