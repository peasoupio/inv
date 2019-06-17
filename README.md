# inv
Intertwined network valuables

*Inv* allows **sequencing** between **intertwined** objects. These objects can be databases, apps, webservices, servers, etc. Anything that need sequencing.

Per example :
ServerA hosts AppA through Kubernetes
ServerB hosts AppB through IIS


```groovy
#~/serverA.groovy
inv {
  name "ServerA"
  
  broadcast inv.Server {
    id [name: "server-a"]
    ready {
      return [
        host: "10.22.99.999",
        install: { service ->
          println "I am installing service ${service}"
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
  
  broadcast inv.Server {
    id [name: "server-b"]
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
  name "ServerB"
  
  require inv.Server(name: "server-a", {
            received: { response ->
              
              assert response
              
              response.install("kubectl")
              
              println "Kubernetes installed on ${response.$owner)
            }
          }
          
  broadcast inv.Kubernetes {
              ready {
                return [
                  http: "http://my-kubernetes.my.host.com",
                  port: "8089", // not by default
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
  
  require inv.Kubernetes {
            resolved { response ->
                response.installPod("my-mod-for-app-3")
            }
          }
}

```groovy
#~/iis.groovy
inv {
  name "iis"
  
  broadcast inv.IIS {
            ready { 
              return [
                deploy: {webApp ->
                  println "Deployed ${webApp}"
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
  
  require inv.IIS {
            received { response ->
              response.deploy("my-web-app")
            }
          }
}
```

These are the elements we can determine :
* Which instance server (barebone or under kubernetes) hosts AppA
* Apps are deployed without knowledge of credentials or physical access points (EP)
