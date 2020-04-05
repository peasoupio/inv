inv {
    name "ServerB"

    broadcast inv.Server using {
        id name: "server-b"
        ready {
            return [
                    host: "10.22.99.998",
            ]
        }
    }
}