inv {
    name "appB"

    require inv.IIS using {
        resolved {
            response.deploy("my-web-app")
        }
    }

    broadcast inv.App(id: 'AppB')
}