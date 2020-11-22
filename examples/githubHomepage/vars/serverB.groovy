inv {
    name "ServerB"

    broadcast { Server } using {
        id name: "server-b"
        ready {
            return [
                    host: "10.22.99.998",
            ]
        }
    }
}