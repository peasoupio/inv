# Dockerfile

# Alpine Linux with OpenJDK JDK
FROM openjdk:8-jdk-alpine

# Install Bash
RUN apk add --no-cache bash

# Copy tar.gz to image
ADD ${project.artifactId}-${project.version}.tar.gz /usr/lib/inv-${project.version}

RUN echo -e '#!/bin/bash' >> /usr/bin/inv && \
    echo -e "java -jar /usr/lib/inv-${project.version}/${project.artifactId}-${project.version}.jar \$@" >> /usr/bin/inv && \
    chmod +x /usr/bin/inv