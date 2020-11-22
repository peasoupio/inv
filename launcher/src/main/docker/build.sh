#!/bin/sh

# Login to Docker
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin

# Move tar gz here
cp "${project.build.directory}/dist/${project.artifactId}-${project.version}.tar.gz" "${project.artifactId}-${project.version}.tar.gz"

# Compile docker image
docker build -t "peasoupio/inv:${project.version}" .

# Get latest ID of the image
LATESTID=$(docker images "peasoupio/inv" -q)

# Tag the image with the ID
docker tag "$LATESTID" "peasoupio/inv:${project.version}"

# Push the image to Dockerhub
docker push "peasoupio/inv:${project.version}"
