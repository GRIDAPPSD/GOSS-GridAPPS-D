#!/bin/bash

usage () {
  /bin/echo "Usage:  $0 -b Build the docker image"
  /bin/echo "           -p Push image to dockerhub"
  exit 2
}

IMAGE="gridappsd/gridappsd:dev"

# parse options
while getopts bp option ; do
  case $option in
    b) # Pass gridappsd tag to docker-compose
      # Docker file on travis relative from root.
      docker build -t $IMAGE .
      ;;
    p) # Pass gridappsd tag to docker-compose
      docker push $IMAGE
      ;;
    *) # Print Usage
      usage
      ;;
  esac
done
shift `expr $OPTIND - 1`

