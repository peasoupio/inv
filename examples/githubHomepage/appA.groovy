inv {
    name "appA"

    require inv.Kubernetes using {
        resolved {
            response.installPod("my-mod-for-app-3")
        }
    }

    broadcast inv.App(id: 'AppA')
}