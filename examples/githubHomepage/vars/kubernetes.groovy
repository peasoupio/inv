inv {
    name "Kubernetes"

    require { Server } using {
        id name: "server-a"

        resolved {
            assert response

            response.install("kubectl")
        }
    }

    broadcast { Kubernetes } using {
        ready {
            return [
                    http      : "http://my-kubernetes.my.host.com",
                    port      : "8089", // not by default
                    installPod: { pod ->
                        println "Pod ${pod} has been installed"
                    }
            ]
        }
    }
}