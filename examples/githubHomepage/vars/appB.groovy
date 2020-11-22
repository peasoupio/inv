inv {
    name "appB"

    require { IIS } using {
        resolved {
            response.deploy("my-web-app")
        }
    }

    broadcast { App(id: 'AppB') }
}