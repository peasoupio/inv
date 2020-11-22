inv {
    name "appA"

    require { Kubernetes } using {
        resolved {
            response.installPod("my-mod-for-app-3")
        }
    }

    broadcast { App(id: 'AppA') }
}