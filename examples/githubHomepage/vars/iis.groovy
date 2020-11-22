inv {
    name "iis"

    require { Server(name: "server-a") }

    broadcast { IIS } using {
        ready {
            return [
                    deploy: {webApp ->
                        println "IIS webapp ${webApp} has been deployed"
                    }
            ]
        }
    }
}