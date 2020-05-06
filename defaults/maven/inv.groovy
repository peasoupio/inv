import org.apache.maven.model.Dependency
@Grab("org.apache.maven:maven-model:3.0.2")
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader

inv {
    require $inv.Files into '$files'

    broadcast $inv.Maven using {
        ready {
            def instance = [:]
            instance << [
                $: {
                    def analyse = instance.analyze as Closure
                    def copy = analyse
                            .dehydrate()
                            .rehydrate(
                                    delegate,
                                    analyse.owner,
                                    analyse.thisObject)
                    copy.resolveStrategy = Closure.DELEGATE_FIRST

                    return copy(path) // using default path of Inv
                },
                analyze: { String pwd, String exclude = "" ->

                    def poms = []

                    // Using find makes it faster
                    for(File file : $files.find(pwd, "pom.xml", exclude)) {

                        MavenXpp3Reader reader = new MavenXpp3Reader()
                        Model model = reader.read(new FileReader(file))

                        broadcast $inv.Artifact using {
                            id model.groupId + ":" + model.artifactId

                            ready { [model: model] }
                        }

                        for(Dependency dep : model.dependencies) {
                            require $inv.Artifact(dep.groupId + ":" + dep.artifactId)
                        }

                        poms << model
                    }

                    return [
                        poms: poms
                    ]
                }
            ]

            return instance
        }
    }
}