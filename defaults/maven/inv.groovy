import org.apache.maven.model.Model
@Grab("org.apache.maven:maven-model:3.0.2")

import org.apache.maven.model.io.xpp3.MavenXpp3Reader

inv {
    require inv.Files into '$files'

    broadcast inv.Maven using {
        ready {
            def instance = [:]
            instance << [
                $: {
                    def copy = (instance.analyze as Closure)
                            .dehydrate()
                            .rehydrate(
                                    delegate,
                                    instance.analyze.owner,
                                    instance.analyze.thisObject)
                    copy.resolveStrategy = Closure.DELEGATE_FIRST

                    copy(path) // using default path of Inv
                },
                analyze: { String pwd, String exclude = "" ->

                    // Using find makes it faster
                    $files.find(pwd, "pom.xml", exclude).each {

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
            ]

            return instance
        }
    }
}