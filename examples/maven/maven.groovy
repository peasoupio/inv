import org.apache.maven.model.Model
@Grab("org.apache.maven:maven-model:3.0.2")

import org.apache.maven.model.io.xpp3.MavenXpp3Reader

inv {
    name "files"

    broadcast { Files } using {
        ready {[
            glob: { String glob ->
                return new FileNameFinder().getFileNames(pwd, glob)
            }
        ]}
    }
}

inv {
    name "maven"

    require { Files } into '$files'

    broadcast { Maven } using {
        ready {[
            analyze: { glob ->

                $files.glob(glob).each {

                    MavenXpp3Reader reader = new MavenXpp3Reader()
                    Model model = reader.read(new FileReader(it))

                    broadcast { Artifact } using {
                        id model.groupId + ":" + model.artifactId

                        ready {
                            model
                        }
                    }

                    model.dependencies.each {
                        require { Artifact(it.groupId + ":" + it.artifactId) }
                    }
                }
            }
        ]}
    }
}

