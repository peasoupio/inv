# Dockerfile

# Alpine Linux with OpenJDK JRE
FROM openjdk:8-jre-alpine

# Copy tar.gz to image
ADD ${project.artifactId}-${project.version}.tar.gz /inv

# Install Bash
RUN apk add --no-cache bash

# Create 'inv' executable file
RUN echo -e '#!/bin/bash' >> /usr/bin/inv && \
    echo -e "VERSION=${project.artifactId}-${project.version}" >>/usr/bin/inv && \
    echo -e 'java -jar /inv/$VERSION/lib/$VERSION.jar "$@"' >> /usr/bin/inv && \
    chmod +x /usr/bin/inv