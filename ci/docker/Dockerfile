# Dockerfile

# Alpine Linux with OpenJDK JDK
FROM openjdk:8-jdk-alpine

# Copy tar.gz to image
ADD ${project.artifactId}-${project.version}.tar.gz /inv

# Install Bash
RUN apk add --no-cache bash

# Create 'inv' executable file
RUN echo -e '#!/bin/bash' >> /usr/bin/inv && \
    echo -e "VERSION=${project.artifactId}-${project.version}" >>/usr/bin/inv && \
    echo -e "JAVAPROPS=-Djava.system.class.loader=groovy.lang.GroovyClassLoader" >>/usr/bin/inv && \
    echo -e 'java $JAVAPROPS -jar /inv/$VERSION/lib/$VERSION.jar "$@"' >> /usr/bin/inv && \
    chmod +x /usr/bin/inv