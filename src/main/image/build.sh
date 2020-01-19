#!/bin/sh

# Login to Docker
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin

# Move tar gz here
cp "${project.build.directory}/${project.artifactId}-${project.version}.tar.gz" "${project.artifactId}-${project.version}.tar.gz"

# Compile docker image
docker build -t "peasoupio/${project.artifactId}:${project.version}" .

# Get latest ID of the image
LATESTID=$(docker images "peasoupio/${project.artifactId}" -q)

# Tag the image with the ID
docker tag "$LATESTID" "peasoupio/${project.artifactId}:${project.version}"

# Push the image to Dockerhub
docker push "peasoupio/${project.artifactId}:${project.version}"
