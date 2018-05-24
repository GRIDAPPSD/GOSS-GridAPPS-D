#!/bin/bash

usage () {
  /bin/echo "Usage:  $0 -b Build the docker image"
  /bin/echo "           -p Push image to dockerhub"
  exit 2
}

TAG="$TRAVIS_BRANCH"

ORG=`echo $DOCKER_PROJECT | tr '[:upper:]' '[:lower:]'`
ORG="${ORG:+${ORG}/}"
IMAGE="${ORG}gridappsd"
TIMESTAMP=`date +'%y%m%d%H'`
GITHASH=`git log -1 --pretty=format:"%h"`

BUILD_VERSION="${TIMESTAMP}_${GITHASH}${TRAVIS_BRANCH:+:$TRAVIS_BRANCH}"
echo "BUILD_VERSION $BUILD_VERSION"

# parse options
while getopts bp option ; do
  case $option in
    b) # Pass gridappsd tag to docker-compose
      # Docker file on travis relative from root.
      docker build --build-arg TIMESTAMP="${BUILD_VERSION}" -t ${IMAGE}:${TIMESTAMP}_${GITHASH} .
      ;;
    p) # Pass gridappsd tag to docker-compose
      if [ -n "$TAG" -a -n "$ORG" ]; then
        echo "docker push ${IMAGE}:${TIMESTAMP}_${GITHASH}"
        docker push ${IMAGE}:${TIMESTAMP}_${GITHASH}
        docker tag ${IMAGE}:${TIMESTAMP}_${GITHASH} ${IMAGE}:$TAG
        echo "docker push ${IMAGE}:$TAG"
        docker push ${IMAGE}:$TAG
      fi
      ;;
    *) # Print Usage
      usage
      ;;
  esac
done
shift `expr $OPTIND - 1`

