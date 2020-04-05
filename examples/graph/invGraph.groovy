import org.apache.maven.model.Model
@Grab("org.apache.maven:maven-model:3.0.2")

import org.apache.maven.model.io.xpp3.MavenXpp3Reader

inv {
    name "appA"

    require inv.Kubernetes using {
        resolved {
            response.installPod("my-mod-for-app-3")
        }
    }
}

inv {
    name "appB"

    require inv.IIS using {
        resolved {
            response.deploy("my-web-app")
        }
    }
}

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

inv {
    name "my-app-1"

    require inv.Maven using {
        resolved {
            impersonate response.analyze withArgs "app1/pom.xml"
        }
    }
}

inv {
    name "my-app-2"

    require inv.Maven using {
        resolved {
            impersonate response.analyze withArgs "app2/pom.xml"

            println "got my poms"
        }
    }

    ready {
        println "my-app-2 is ready!"
    }
}


inv {
    name "files"

    broadcast inv.Files using {
        ready {[
            glob: { String glob ->
                return new FileNameFinder().getFileNames(pwd, glob)
            }
        ]}
    }
}

inv {
    name "maven"

    require inv.Files into '$files'

    broadcast inv.Maven using {
        ready {[
            analyze: { glob ->

                $files.glob(glob).each {

                    MavenXpp3Reader reader = new MavenXpp3Reader()
                    Model model = reader.read(new FileReader(it))

                    broadcast inv.Artifact using {
                        id model.groupId + ":" + model.artifactId

                        ready {
                            model
                        }
                    }

                    model.dependencies.each {
                        require inv.Artifact(it.groupId + ":" + it.artifactId)
                    }
                }
            }
        ]}
    }
}




