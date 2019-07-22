# inv
Intertwined network valuables

*Inv* allows **sequencing** between **intertwined** objects. These objects can be databases, apps, webservices, servers, etc. Anything that need sequencing.

Get latest version here : [Version 0.2-beta - TESTS PURPOSES ONLY](https://github.com/peasoupio/inv/releases/download/0.2-beta/inv-0.2-beta-SNAPSHOT.zip) 

## Quick example :

### What we know ?
* ServerA hosts AppA through Kubernetes
* ServerB hosts AppB through IIS

### How we write it up ?

For more information, [get there](https://github.com/peasoupio/inv/wiki/Syntax)

```groovy
#~/serverA.groovy
inv {
    name "ServerA"

    broadcast inv.Server using {
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
```

```groovy
#~/serverB.groovy
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
```

```groovy
#~/kubernetes.groovy
inv {
    name "Kubernetes"

    require inv.Server using {
        id name: "server-a"

        resolved {
            assert response

            response.install("kubectl")
        }
    }

    broadcast inv.Kubernetes using {
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
```

```groovy
#~/appA.groovy
inv {
    name "appA"

    require inv.Kubernetes using {
        resolved {
            response.installPod("my-mod-for-app-3")
        }
    }
}
```

```groovy
#~/iis.groovy
inv {
    name "iis"

    require inv.Server(name: "server-a")

    broadcast inv.IIS using {
        ready {
            return [
                    deploy: {webApp ->
                        println "IIS webapp ${webApp} has been deployed"
                    }
            ]
        }
    }
}
```

```groovy
#~/appB.groovy
inv {
    name "appB"

    require inv.IIS using {
        resolved {
            response.deploy("my-web-app")
        }
    }
}
```

```
inv ~/*.groovy

> [INV] ---- [DIGEST] #1 ----
> [INV] [BROADCAST] [Server] [name:server-a]
> [INV] [BROADCAST] [Server] [name:server-b]
> [INV] ---- [DIGEST] #2 ----
> [INV] [REQUIRE] [Server] [name:server-a]
> [INV] [BROADCAST] [IIS] undefined
> [INV] [REQUIRE] [Server] [name:server-a]
> [INV] [BROADCAST] [Kubernetes] undefined
> Installing service kubectl on 10.22.99.999
> [INV] ---- [DIGEST] #3 ----
> [INV] [REQUIRE] [Kubernetes] undefined
> [INV] [REQUIRE] [IIS] undefined
> Pod my-mod-for-app-3 has been installed
> IIS webapp my-web-app has been deployed
> [INV] --- completed ----
```

These are the elements we can determine :
* Which instance server (barebone or under kubernetes) hosts AppA
* Apps are deployed without knowledge of credentials or physical access points (EP)

### Contribution
First and only global rule : use your common sense - we help each other :)
