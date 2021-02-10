package io.peasoup.inv.loader

@Grab("org.apache.maven:maven-model:3.0.2")
import java.lang.Object

// use this class so IntelliJ won't alert saying "org.apache.maven.model.Model" does not exist

inv {
    def modelClazz = getClass().classLoader.loadClass("org.apache.maven.model.Model")
    debug(modelClazz.name)
}



