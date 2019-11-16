# inv
Intertwined network valuables

*Inv* allows **sequencing** between **intertwined** objects. These objects can be databases, apps, webservices, servers, etc. Anything that need sequencing.

Get latest version here : [Version 0.4-beta](https://github.com/peasoupio/inv/releases/download/0.4-beta/inv-0.4-beta-SNAPSHOT.zip)  

![Travis build Status](https://travis-ci.org/peasoupio/inv.svg?branch=feature%2F0.4-beta)
[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=peasoupio_inv&metric=alert_status)](https://sonarcloud.io/dashboard?id=peasoupio_inv)

#### Maven coordinates:

```
<dependency>
    <groupId>io.peasoup</groupId>
    <artifactId>inv</artifactId>
    <version>0.4-beta-SNAPSHOT</version>
</dependency>
```
(NOTE : Snapshots need to be downloaded from this repository: https://oss.sonatype.org/content/repositories/snapshots/) 

#### Prerequisites  
JDK 8 or higher

## Avaiable commands:  
```
INV - Generated a INV sequence and manage past generations
Generate a new sequence:
usage: inv [options] <pattern>...
Options:
 <pattern>   Execute an Ant-compatible file pattern
             (p.e *.groovy, ./**/*.groovy, ...)

             Pattern is expandable using a space-separator
             (p.e myfile1.groovy myfile2.groovy)
           
 -e,--exclude <label>   Exclude files if containing the label
 -s,--from-scm <file>   Process the SCM file to extract or update sources
 -x,--debug             Enable debug logs
Manage or view an old sequence:
usage: inv [options]
 -d,--delta <previousFile>   Generate a delta from a recent execution in
                             STDIN compared to a previous execution
 -g,--graph <type>           Print the graph from STDIN of a previous
                             execution
 -h,--html                   Output generates an HTML file
```

## Quick example:

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
inv.sh ./example/githubHomepage/*.groovy

[INV] file: ./example/githubHomepage/appA.groovy
[INV] file: ./example/githubHomepage/appB.groovy
[INV] file: ./example/githubHomepage/iis.groovy
[INV] file: ./example/githubHomepage/kubernetes.groovy
[INV] file: ./example/githubHomepage/serverA.groovy
[INV] file: ./example/githubHomepage/serverB.groovy
[INV] ---- [DIGEST] started ----
[INV] ---- [DIGEST] #1 (state=RUNNING) ----
[INV] [ServerA] => [BROADCAST] [Server] [name:server-a]
[INV] [ServerB] => [BROADCAST] [Server] [name:server-b]
[INV] ---- [DIGEST] #2 (state=RUNNING) ----
[INV] [iis] => [REQUIRE] [Server] [name:server-a]
[INV] [iis] => [BROADCAST] [IIS] undefined
[INV] [Kubernetes] => [REQUIRE] [Server] [name:server-a]
[INV] [Kubernetes] => [BROADCAST] [Kubernetes] undefined
Installing service kubectl on 10.22.99.999
[INV] ---- [DIGEST] #3 (state=RUNNING) ----
[INV] [appA] => [REQUIRE] [Kubernetes] undefined
[INV] [appB] => [REQUIRE] [IIS] undefined
Pod my-mod-for-app-3 has been installed
IIS webapp my-web-app has been deployed
[INV] ---- [DIGEST] completed ----
```

These are the elements we can determine :
* Which instance server (barebone or under kubernetes) hosts AppA
* Apps are deployed without knowledge of credentials or physical access points (EP)

### Graphs

You could also generate a graph from a previous execution. Per example :

    cat myprevious.log | ./inv graph dot

could generate this dot file

```
digraph inv {
	"ServerA";
	"ServerB";
	"iis";
	"Kubernetes";
	"files";
	"maven";
	"my-app-1";
	"my-app-2";
	"appA";
	"appB";
    
	"iis" -> "ServerA" [ label = "[Server] [name:server-a]" ];
	"Kubernetes" -> "ServerA" [ label = "[Server] [name:server-a]" ];
	"maven" -> "files" [ label = "[Files]" ];
	"my-app-1" -> "maven" [ label = "[Maven]" ];
	"my-app-2" -> "maven" [ label = "[Maven]" ];
	"my-app-2" -> "my-app-1" [ label = "[Artifact] com.mycompany.app:my-app-1" ];
	"appA" -> "Kubernetes" [ label = "[Kubernetes]" ];
	"appB" -> "iis" [ label = "[IIS]" ];
}
```

This is an image represention of the dot file :  
![Dotgraph image using WebGraphViz](https://github.com/peasoupio/inv/blob/feature/0.4-beta/src/main/example/graph/dotGraph.png "Dotgraph image using WebGraphViz")


### Defaults INV
We offer default implementation of multi-purposes INV, such as "files (I/O), http, ..."  
You can get them at "./defaults".  
This is how you would import "files" using an SCM file:  
```groovy
#~/scm.groovy

"default-files" {
    path "choose your path..."
    scm  "https://github.com/peasoupio/inv.git"
    entry "defaults/files/inv.groovy"
    hooks {
        init {
            "git clone ${scm}"
        }
        update {
            "git pull"
        }
    }
}
...
```
NOTE: Using the same "path" allows you to do a single extraction

### Contribution
First and only global rule : use your common sense - we help each other :)

### Known issues
#### Create symlink under Windows
Please enable SeCreateSymbolicLinkPrivilege (using Developer mode OR look at https://superuser.com/questions/124679/how-do-i-create-a-link-in-windows-7-home-premium-as-a-regular-user/125981#125981)