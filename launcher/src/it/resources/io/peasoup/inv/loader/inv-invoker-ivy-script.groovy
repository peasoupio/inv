package io.peasoup.inv.loader

@Grab("org.apache.maven:maven-model:3.0.2")
import org.apache.maven.model.Model

inv {
    def modelClazz = getClass().classLoader.loadClass("org.apache.maven.model.Model")
    debug(modelClazz.name)
}



