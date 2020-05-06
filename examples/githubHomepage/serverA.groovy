inv {
    name "ServerA"

    broadcast $inv.Server using {
        id name: "server-a"
        ready {
            return [
                    host: "10.22.99.999",
                    install: { service ->
                        println "Installing service ${service} on 10.22.99.999"
                    }
            ]
        }
    }
}